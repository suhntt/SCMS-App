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

    static sentimentInstance = null;
    static async getSentimentInstance() {
        if (this.sentimentInstance === null) {
            // Load a specialized sentiment analysis model
            this.sentimentInstance = await pipeline('sentiment-analysis', 'Xenova/distilbert-base-uncased-finetuned-sst-2-english');
        }
        return this.sentimentInstance;
    }

    static async classifyComplaint(description) {
        try {
            const classifier = await this.getInstance();
            const sentimentAnalyzer = await this.getSentimentInstance();

            // 1. Run Classification (Zero-Shot)
            const candidateLabels = ['Infrastructure', 'Sanitation', 'Emergency', 'Traffic', 'Pollution', 'Noise'];
            const output = await classifier(description, candidateLabels);
            const topCategory = output.labels[0];

            // 2. Run Sentiment Analysis (Deep Learning)
            const sentimentResult = await sentimentAnalyzer(description);
            const sentiment = sentimentResult[0].label; // 'POSITIVE' or 'NEGATIVE'
            const sentimentScore = sentimentResult[0].score;

            // 3. Intelligent Severity Heuristic
            // Combines Category + Semantic Keywords + Sentiment Distress
            let severity = 'Low';
            const highSevCategories = ['Emergency', 'Traffic', 'Pollution'];
            
            // Highly Negative sentiment (>85% confidence) usually indicates high distress or urgency
            const isHighDistress = (sentiment === 'NEGATIVE' && sentimentScore > 0.85);
            const hasUrgentKeywords = description.toLowerCase().includes('urgent') || 
                                     description.toLowerCase().includes('danger') || 
                                     description.toLowerCase().includes('help') ||
                                     description.toLowerCase().includes('broken');

            if (highSevCategories.includes(topCategory) || hasUrgentKeywords || isHighDistress) {
                severity = 'High';
            } else if (['Infrastructure', 'Sanitation'].includes(topCategory)) {
                severity = 'Medium';
            }

            return {
                suggestedCategory: topCategory,
                severity: severity,
                confidence: output.scores[0],
                sentiment: sentiment,
                sentimentScore: sentimentScore
            };
        } catch (e) {
            console.error("AI Classification Error:", e);
            return { suggestedCategory: "Other", severity: "Medium", confidence: 0, sentiment: "NEUTRAL", sentimentScore: 0 };
        }
    }
}

module.exports = AIClassifier;
