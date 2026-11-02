package com.marketingfacil.service;

import com.marketingfacil.domain.AdScript;
import com.marketingfacil.domain.Campaign;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class ArtworkService {
    public void createDesign(Path original, Path destination, Campaign campaign, AdScript script,
                             int width, int height) throws IOException {
        BufferedImage source = ImageIO.read(original.toFile());
        if (source == null) {
            throw new IOException("No se pudo leer la fotografía original.");
        }

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(247, 248, 250));
            graphics.fillRect(0, 0, width, height);

            int margin = Math.max(28, width / 24);
            int panelHeight = Math.max(210, (int) (height * 0.44));
            int imageAreaHeight = height - panelHeight - margin * 2;
            drawContainedImage(graphics, source, margin, margin, width - margin * 2, imageAreaHeight);

            int panelY = margin + imageAreaHeight + margin;
            graphics.setColor(new Color(255, 255, 255));
            graphics.fillRoundRect(margin, panelY, width - margin * 2, panelHeight, 24, 24);
            int textX = margin * 2;
            int textWidth = width - margin * 4;
            int shortSide = Math.min(width, height);
            int companySize = Math.max(12, shortSide / 55);
            int titleSize = Math.max(16, shortSide / 32);
            int bodySize = Math.max(12, shortSide / 55);
            int gap = Math.max(5, shortSide / 100);
            List<String> companyLines;
            List<String> titleLines;
            List<String> bodyLines;
            List<String> actionLines;
            int availableHeight = panelHeight - Math.max(24, margin);
            do {
                Font companyFont = new Font(Font.SANS_SERIF, Font.BOLD, companySize);
                Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, titleSize);
                Font bodyFont = new Font(Font.SANS_SERIF, Font.PLAIN, bodySize);
                companyLines = wrap(graphics, campaign.getCompanyName(), companyFont, textWidth);
                titleLines = wrap(graphics, script.getTitle(), titleFont, textWidth);
                bodyLines = wrap(graphics, script.getBody(), bodyFont, textWidth);
                actionLines = wrap(graphics, script.getCallToAction(), bodyFont, textWidth);
                int textHeight = lineHeight(graphics, companyFont, companyLines.size())
                        + lineHeight(graphics, titleFont, titleLines.size())
                        + lineHeight(graphics, bodyFont, bodyLines.size())
                        + lineHeight(graphics, bodyFont, actionLines.size()) + gap * 3;
                if (textHeight <= availableHeight || (companySize <= 10 && titleSize <= 12 && bodySize <= 10)) break;
                companySize = Math.max(10, companySize - 1);
                titleSize = Math.max(12, titleSize - 1);
                bodySize = Math.max(10, bodySize - 1);
                gap = Math.max(3, gap - 1);
            } while (true);

            int baseline = panelY + Math.max(24, (panelHeight - availableHeight) / 2);
            graphics.setColor(new Color(74, 83, 98));
            Font companyFont = new Font(Font.SANS_SERIF, Font.BOLD, companySize);
            graphics.setFont(companyFont);
            baseline = drawLines(graphics, companyLines, textX, baseline);
            baseline += gap;

            graphics.setColor(new Color(27, 38, 57));
            Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, titleSize);
            graphics.setFont(titleFont);
            baseline = drawLines(graphics, titleLines, textX, baseline);
            baseline += gap;

            graphics.setColor(new Color(59, 67, 80));
            Font bodyFont = new Font(Font.SANS_SERIF, Font.PLAIN, bodySize);
            graphics.setFont(bodyFont);
            baseline = drawLines(graphics, bodyLines, textX, baseline);
            baseline += gap;

            graphics.setColor(new Color(29, 87, 154));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, bodySize));
            drawLines(graphics, actionLines, textX, baseline);
        } finally {
            graphics.dispose();
            source.flush();
        }

        if (!ImageIO.write(canvas, "png", destination.toFile())) {
            throw new IOException("No se pudo guardar uno de los diseños.");
        }
        canvas.flush();
    }

    private void drawContainedImage(Graphics2D graphics, BufferedImage source, int x, int y, int boxWidth, int boxHeight) {
        double scale = Math.min((double) boxWidth / source.getWidth(), (double) boxHeight / source.getHeight());
        int imageWidth = Math.max(1, (int) (source.getWidth() * scale));
        int imageHeight = Math.max(1, (int) (source.getHeight() * scale));
        int imageX = x + (boxWidth - imageWidth) / 2;
        int imageY = y + (boxHeight - imageHeight) / 2;
        graphics.drawImage(source, imageX, imageY, imageWidth, imageHeight, null);
    }

    private List<String> wrap(Graphics2D graphics, String text, Font font, int maxWidth) {
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics();
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split("\\s+")) {
                String remaining = word;
                while (!remaining.isEmpty()) {
                    String candidate = line.isEmpty() ? remaining : line + " " + remaining;
                    if (metrics.stringWidth(candidate) <= maxWidth) {
                        line = new StringBuilder(candidate);
                        remaining = "";
                    } else if (!line.isEmpty()) {
                        lines.add(line.toString());
                        line = new StringBuilder();
                    } else {
                        int splitAt = 1;
                        while (splitAt < remaining.length()
                                && metrics.stringWidth(remaining.substring(0, splitAt + 1)) <= maxWidth) {
                            splitAt++;
                        }
                        lines.add(remaining.substring(0, splitAt));
                        remaining = remaining.substring(splitAt);
                    }
                }
            }
            if (!line.isEmpty()) lines.add(line.toString());
        }
        return lines;
    }

    private int lineHeight(Graphics2D graphics, Font font, int lines) {
        graphics.setFont(font);
        return graphics.getFontMetrics().getHeight() * lines;
    }

    private int drawLines(Graphics2D graphics, List<String> lines, int x, int baseline) {
        int lineHeight = graphics.getFontMetrics().getHeight();
        for (String line : lines) {
            graphics.drawString(line, x, baseline);
            baseline += lineHeight;
        }
        return baseline;
    }
}
