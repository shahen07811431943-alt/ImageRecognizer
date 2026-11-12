package com.example.mycompose.imagerecognizer.data

object Matcher {

    /** الترتيب: المطابق بالرقم أولاً (مؤكد)، ثم أقرب 3 حيوانات بالشكل (تقريبي). */
    fun rank(animals: List<Animal>, r: Analyzer.Result): List<Candidate> {
        val byNumber = animals.filter { it.number.trim().uppercase() in r.tokens }
        val numberCands = byNumber.map { Candidate(it, 1f, true) }
        val visual = animals
            .filter { a -> byNumber.none { it.id == a.id } }
            .mapNotNull { a ->
                a.features.maxOfOrNull { Analyzer.similarity(it, r.features) }
                    ?.let { Candidate(a, it, false) }
            }
            .sortedByDescending { it.score }
            .take(3)
        return numberCands + visual
    }
}
