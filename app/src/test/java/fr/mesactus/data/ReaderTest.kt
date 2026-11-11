package fr.mesactus.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTest {
    @Test
    fun categories() {
        assertTrue(Category.SPORT in Category.classify("Le Real Madrid gagne le match contre Barcelone", "", emptyList()))
        assertTrue(Category.POLITIQUE in Category.classify("El PSOE y el PP pactan en el Ayuntamiento de Granada", "", emptyList()))
        assertTrue(Category.FAITS_DIVERS in Category.classify("Incendie à Orléans : un suspect interpellé par la police", "", emptyList()))
        // « pp » doit être un mot entier
        assertFalse(Category.POLITIQUE in Category.classify("Une nouvelle application pour smartphone", "", emptyList()))
    }
}
