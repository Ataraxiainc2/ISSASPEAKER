package com.issasoda.issaspeaker.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.issasoda.issaspeaker.R
import kotlin.random.Random

/**
 * The typefaces the app draws from (catalog F403, decisions D2, D9 and C20): each text role gets a different
 * family, picked at random ([FontAssignment]); [FontSession] decides when to draw again.
 *
 * Every family is a text face chosen for legibility at reading sizes: open apertures, a generous
 * x-height, lining figures, and distinct shapes for the letters people confuse (I l 1, O 0, rn m).
 * Left out on purpose: display and script faces, monospaced faces, very condensed or very wide faces,
 * faces with old-style figures by default (Raleway, Alegreya) and faces the ISSASODA font rules call out
 * for small text (Quicksand, display serifs).
 *
 * Fonts come from the Play Services provider, never bundled (skill android-google-fonts). Each family is
 * requested at 400 and 700 only; Compose matches the other weights to the nearest of those two.
 * GoogleFontsTest (androidTest) asks the phone's provider for every entry: a family that is not served
 * is removed from this list, not worked around.
 */
object FontPool {

    val families: List<String> = listOf(
        // Designed for legibility or accessibility first
        "Atkinson Hyperlegible", "Parkinsans", "Lexend", "Lexend Deca", "Andika",
        "Readex Pro",
        // Humanist and neo-grotesque sans
        "Inter", "Inter Tight", "Source Sans 3", "Figtree", "Nunito Sans", "Nunito", "Noto Sans", "Open Sans",
        "Roboto", "Roboto Flex", "Lato", "IBM Plex Sans", "Public Sans", "Work Sans", "DM Sans", "Manrope",
        "Plus Jakarta Sans", "Outfit", "Sora", "Space Grotesk", "Red Hat Text", "Red Hat Display", "Mulish",
        "Karla", "Rubik", "Heebo", "Assistant", "Hind", "Barlow", "Fira Sans", "PT Sans", "Ubuntu", "Cabin",
        "Oxygen", "Asap", "Archivo", "Overpass", "Encode Sans", "Libre Franklin", "Montserrat", "Poppins",
        "Commissioner", "Epilogue", "Be Vietnam Pro", "Albert Sans", "Onest", "Golos Text", "Geologica",
        "Instrument Sans", "Hanken Grotesk", "Schibsted Grotesk", "Bricolage Grotesque", "Wix Madefor Text",
        "Reddit Sans", "Gantari", "Radio Canada", "Signika", "Mukta", "Catamaran", "Sarabun", "Chivo",
        "Exo 2", "Titillium Web", "Jost", "Urbanist", "Merriweather Sans", "Anek Latin", "Familjen Grotesk",
        "Mona Sans", "Funnel Sans", "Afacad", "SUSE", "Host Grotesk", "Kumbh Sans", "Noto Sans Display",
        "Ysabeau Office", "Ysabeau Infant", "Proza Libre", "Sarala", "Istok Web", "Arimo", "Carlito",
        "Pontano Sans", "Mada", "Rethink Sans", "Sofia Sans", "Prompt", "Inria Sans", "Rosario", "Martel Sans",
        "Gabarito", "Spline Sans", "Arsenal", "Cantarell", "Gudea", "Sintony", "Varta",
        "Geist", "Ubuntu Sans", "Hubot Sans", "Kanit", "Niramit", "K2D", "Livvic", "Glory", "Lunasima",
        "Alexandria", "Saira", "Maven Pro", "Mitr",
        // Text serifs and slabs
        "Source Serif 4", "Merriweather", "Literata", "Lora", "Noto Serif", "PT Serif", "IBM Plex Serif",
        "Libre Baskerville", "Spectral", "Newsreader", "Roboto Serif", "Gelasio", "Bitter", "Domine",
        "Zilla Slab", "Roboto Slab", "Faustina", "Petrona", "Besley", "Andada Pro", "Vollkorn", "Charis SIL",
        "Gentium Book Plus", "Tinos", "Noticia Text", "Frank Ruhl Libre", "Brygada 1918", "Libre Caslon Text",
        "Martel", "Arvo", "Lusitana", "Crimson Pro", "Crimson Text", "Piazzolla", "Hahmlet", "Abhaya Libre",
        "Gupter", "Sumana", "Aleo", "Podkova", "Kameron", "Rokkitt",
    )

    /** The two weights requested per family; GoogleFontsTest checks exactly these. */
    val weights: List<FontWeight> = listOf(FontWeight.Normal, FontWeight.Bold)

    fun draw(): FontAssignment = FontAssignment.draw(families, Random.Default)

    private val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.issa_fonts_certs,
    )

    /** While the font downloads (or if it never arrives) Compose draws the system sans: the UI never waits. */
    fun fontFamily(name: String): FontFamily = FontFamily(weights.map { Font(GoogleFont(name), provider, it) })
}
