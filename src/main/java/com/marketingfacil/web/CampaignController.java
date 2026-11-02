package com.marketingfacil.web;

import com.marketingfacil.domain.Campaign;
import com.marketingfacil.service.CampaignService;
import com.marketingfacil.service.FileStorageService;
import com.marketingfacil.service.GenerationService;
import com.marketingfacil.service.PublicationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CampaignController {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final int MAX_SIDE = 6000;
    private static final long MAX_PIXELS = 20_000_000L;

    private final CampaignService campaigns;
    private final GenerationService generation;
    private final FileStorageService files;
    private final PublicationService publication;

    public CampaignController(CampaignService campaigns, GenerationService generation,
                              FileStorageService files, PublicationService publication) {
        this.campaigns = campaigns;
        this.generation = generation;
        this.files = files;
        this.publication = publication;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("campaigns", campaigns.recentCampaigns());
        return "index";
    }

    @PostMapping("/campaigns")
    public String create(@RequestParam @NotBlank @Size(max = 120) String companyName,
                         @RequestParam @NotBlank @Size(max = 120) String productName,
                         @RequestParam @NotBlank @Size(max = 500) String description,
                         @RequestParam("photo") MultipartFile photo,
                         RedirectAttributes redirect) {
        try {
            validateText(companyName, "El nombre de la empresa", 120);
            validateText(productName, "El nombre del producto", 120);
            validateText(description, "La descripción", 500);
            String extension = validateImage(photo);
            String storedName = files.storeOriginal(photo, extension);
            Campaign campaign = campaigns.create(companyName, productName, description, storedName);
            redirect.addFlashAttribute("notice", "Campaña creada. Elige un guion para continuar.");
            return "redirect:/campaigns/" + campaign.getId();
        } catch (IllegalArgumentException | IOException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            redirect.addFlashAttribute("companyName", companyName);
            redirect.addFlashAttribute("productName", productName);
            redirect.addFlashAttribute("description", description);
            return "redirect:/";
        }
    }

    @GetMapping("/campaigns/{id}")
    public String show(@PathVariable Long id, Model model) {
        Campaign campaign = campaigns.find(id);
        model.addAttribute("campaign", campaign);
        model.addAttribute("selectedScript", campaign.getScripts().stream()
                .filter(script -> script.getId().equals(campaign.getSelectedScriptId()))
                .findFirst().orElse(null));
        return "campaign";
    }

    @PostMapping("/campaigns/{id}/select")
    public String chooseScript(@PathVariable Long id, @RequestParam Long scriptId, RedirectAttributes redirect) {
        try {
            campaigns.chooseScript(id, scriptId);
            redirect.addFlashAttribute("notice", "Guion elegido. Confírmalo en el paso 3 para iniciar la generación.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/campaigns/" + id;
    }

    @PostMapping("/campaigns/{id}/generate")
    public String generate(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            generation.queueGeneration(id);
            redirect.addFlashAttribute("notice", "Generación iniciada. Puedes consultar el estado en esta página.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/campaigns/" + id;
    }

    @PostMapping("/campaigns/{id}/simulate")
    public String simulate(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            publication.simulate(id);
            redirect.addFlashAttribute("notice", "Simulación registrada. No se publicó contenido en ninguna red.");
        } catch (IllegalStateException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/campaigns/" + id;
    }

    @GetMapping("/campaigns/{id}/status")
    public ResponseEntity<StatusResponse> status(@PathVariable Long id) {
        Campaign campaign = campaigns.find(id);
        return ResponseEntity.ok(new StatusResponse(campaign.getGenerationStatus(), campaign.getGenerationError()));
    }

    @GetMapping("/campaigns/{id}/files/{kind}")
    public ResponseEntity<byte[]> file(@PathVariable Long id, @PathVariable String kind,
                                       @RequestParam(defaultValue = "false") boolean download) throws IOException {
        Campaign campaign = campaigns.find(id);
        String filename = switch (kind) {
            case "original" -> campaign.getOriginalImage();
            case "square" -> campaign.getSquareImage();
            case "vertical" -> campaign.getVerticalImage();
            case "horizontal" -> campaign.getHorizontalImage();
            case "video" -> campaign.getVideoFile();
            case "text" -> null;
            default -> throw new IllegalArgumentException("Archivo no válido.");
        };
        if ("text".equals(kind)) {
            return textFile(campaign);
        }
        if (filename == null) throw new IllegalArgumentException("Este archivo todavía no está disponible.");

        Path path = files.resolve(filename);
        if (!Files.isRegularFile(path)) throw new IllegalArgumentException("El archivo solicitado no está disponible.");
        MediaType type = filename.endsWith(".mp4") ? MediaType.parseMediaType("video/mp4")
                : filename.endsWith(".jpg") ? MediaType.IMAGE_JPEG : MediaType.IMAGE_PNG;
        String disposition = (download ? "attachment" : "inline") + "; filename=\"" + filename + "\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(type)
                .body(Files.readAllBytes(path));
    }

    private ResponseEntity<byte[]> textFile(Campaign campaign) {
        String text = campaign.getScripts().stream()
                .filter(script -> script.getId().equals(campaign.getSelectedScriptId()))
                .findFirst()
                .map(script -> script.getTitle() + "\n\n" + script.getBody() + "\n\n" + script.getCallToAction() + "\n")
                .orElse(campaign.getScripts().stream()
                        .map(script -> script.getStyle() + " — " + script.getTitle() + "\n" + script.getBody() + "\n" +
                                script.getCallToAction())
                        .reduce((first, second) -> first + "\n\n---\n\n" + second).orElse(""));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"campana-" + campaign.getId() + ".txt\"")
                .contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                .body(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String validateImage(MultipartFile photo) throws IOException {
        if (photo.isEmpty()) throw new IllegalArgumentException("Selecciona una fotografía.");
        if (photo.getSize() > MAX_BYTES) throw new IllegalArgumentException("La fotografía no puede superar 10 MB.");

        try (ImageInputStream input = ImageIO.createImageInputStream(photo.getInputStream())) {
            if (input == null) throw new IllegalArgumentException("El archivo no es una imagen JPG o PNG válida.");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("El archivo no es una imagen JPG o PNG válida.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png")) {
                    throw new IllegalArgumentException("Solo se aceptan imágenes JPG y PNG.");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_SIDE || height > MAX_SIDE || (long) width * height > MAX_PIXELS) {
                    throw new IllegalArgumentException("La imagen debe medir como máximo 6000 px por lado y 20 megapíxeles.");
                }
                if (reader.read(0) == null) throw new IllegalArgumentException("No se pudo decodificar la imagen.");
                return format.equals("png") ? ".png" : ".jpg";
            } finally {
                reader.dispose();
            }
        } catch (RuntimeException exception) {
            if (exception instanceof IllegalArgumentException) throw exception;
            throw new IllegalArgumentException("No se pudo validar la imagen. Usa un JPG o PNG válido.", exception);
        }
    }

    private void validateText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " es obligatorio.");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(label + " no puede superar " + maxLength + " caracteres.");
        }
    }

    public record StatusResponse(String status, String error) {
    }
}
