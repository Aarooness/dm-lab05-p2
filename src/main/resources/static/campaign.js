(() => {
    const page = document.body;
    if (page.dataset.generationStatus !== "GENERATING") return;

    const id = page.dataset.campaignId;
    const status = document.getElementById("generation-status");
    const error = document.getElementById("generation-error");
    const timer = window.setInterval(async () => {
        try {
            const response = await fetch(`/campaigns/${id}/status`, { headers: { Accept: "application/json" } });
            if (!response.ok) throw new Error("No se pudo consultar el estado de generación.");
            const result = await response.json();
            status.textContent = ({
                PENDING: "Pendiente",
                GENERATING: "Generando",
                READY: "Listo",
                ERROR: "Error"
            })[result.status] || "Error";
            if (result.status === "READY" || result.status === "ERROR") {
                window.clearInterval(timer);
                window.location.reload();
            }
        } catch (failure) {
            window.clearInterval(timer);
            status.textContent = "No se pudo consultar";
            if (error) error.textContent = failure.message;
        }
    }, 2500);
})();
