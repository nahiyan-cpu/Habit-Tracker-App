package com.example.domain

data class MotivationalQuote(
    val text: String,
    val author: String
)

object QuotesProvider {
    val quotes: List<MotivationalQuote> = listOf(
        MotivationalQuote("We are what we repeatedly do. Excellence, then, is not an act, but a habit.", "Will Durant"),
        MotivationalQuote("Small habits compound into remarkable transformations over time.", "James Clear"),
        MotivationalQuote("Success is the sum of small efforts, repeated day in and day out.", "Robert Collier"),
        MotivationalQuote("Motivation gets you started. Consistency keeps you growing.", "Habitly"),
        MotivationalQuote("You do not rise to the level of your goals; you fall to the level of your systems.", "James Clear"),
        MotivationalQuote("Discipline is choosing between what you want now and what you want most.", "Abraham Lincoln"),
        MotivationalQuote("A year from now, you may wish you had started today.", "Karen Lamb"),
        MotivationalQuote("Focus on progress, not perfection. Every single check-in counts.", "Habitly"),
        MotivationalQuote("The secret of your future is hidden in your daily routine.", "Mike Murdock"),
        MotivationalQuote("Consistency is the quiet engine behind every breakthrough.", "Habitly"),
        MotivationalQuote("Start where you are. Use what you have. Do what you can.", "Arthur Ashe"),
        MotivationalQuote("Great things are done by a series of small things brought together.", "Vincent van Gogh"),
        MotivationalQuote("Don't break the chain. Show up for yourself today.", "Habitly"),
        MotivationalQuote("Your habits are the architecture of your everyday life.", "Habitly"),
        MotivationalQuote("One positive choice today makes tomorrow's choice easier.", "Habitly"),
        MotivationalQuote("It does not matter how slowly you go as long as you do not stop.", "Confucius")
    )

    fun nextQuote(lastIndex: Int): Pair<Int, MotivationalQuote> {
        if (quotes.isEmpty()) return 0 to MotivationalQuote("Keep showing up every day.", "Habitly")
        val nextIdx = (lastIndex + 1).mod(quotes.size)
        return nextIdx to quotes[nextIdx]
    }
}
