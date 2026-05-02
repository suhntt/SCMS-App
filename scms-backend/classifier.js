const { pipeline } = require('@xenova/transformers');

class AIClassifier {
    static instance = null;

    static async getInstance() {
        if (this.instance === null) {
            // Load a lightweight zero-shot classification model from HuggingFace
            // This runs locally on CPU (or WASM equivalent) without external APIs!
            this.instance = await pipeline('zero-shot-classification', 'Xenova/mobilebert-uncased-mnli');
        }
        return this.instance;
    }

    static async classifyComplaint(description) {
        try {
            const classifier = await this.getInstance();

            const candidateLabels = ['Infrastructure', 'Sanitation', 'Emergency', 'Traffic', 'Pollution', 'Noise'];
            const output = await classifier(description, candidateLabels);

            // Determine category (Highest probability score)
            const topCategory = output.labels[0];

            // Determine severity heuristically based on the top category and keywords, 
            // or optionally run another sentiment pipeline. 
            // For now, Emergency and Traffic problems inherently score High.
            let severity = 'Low';
            const highSevCategories = ['Emergency', 'Traffic', 'Pollution'];
            if (highSevCategories.includes(topCategory) || description.toLowerCase().includes('urgent') || description.toLowerCase().includes('danger')) {
                severity = 'High';
            } else if (['Infrastructure', 'Sanitation'].includes(topCategory)) {
                severity = 'Medium';
            }

            return {
                suggestedCategory: topCategory,
                severity: severity,
                confidence: output.scores[0]
            };
        } catch (e) {
            console.error("AI Classification Error:", e);
            return { suggestedCategory: "Other", severity: "Medium", confidence: 0 };
        }
    }
}

module.exports = AIClassifier;
