package dev.nenoeldeeb.education.absencerecord.domain.repositories

interface StorageRepository {
    /**
     * Reads a text file, refusing anything larger than [maxBytes].
     *
     * The limit is enforced by reading at most `maxBytes + 1` bytes and failing when
     * the extra byte arrives, so an oversize file is never held in memory in full.
     * Without this an import of a hostile file exhausts the heap before any
     * record-level cap can be consulted (FR-063, SC-010).
     */
    suspend fun readTextFromUri(
        uriString: String,
        maxBytes: Long = Long.MAX_VALUE
    ): Result<String>

    suspend fun writeTextToUri(
        uriString: String,
        text: String
    ): Result<Unit>
}