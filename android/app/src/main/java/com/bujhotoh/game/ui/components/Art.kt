package com.bujhotoh.game.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bujhotoh.game.R

/**
 * 3D illustrations for the built-in decks (Microsoft Fluent Emoji 3D, MIT licence, see
 * assets/licenses). Keyed by category id; custom decks fall back to their emoji.
 */
object DeckArt {
    private val byId: Map<String, Int> = mapOf(
        "animals" to R.drawable.art_animals, "objects" to R.drawable.art_objects, "food" to R.drawable.art_food,
        "famous" to R.drawable.art_famous, "jobs" to R.drawable.art_jobs, "sports" to R.drawable.art_sports,
        "places" to R.drawable.art_places, "countries" to R.drawable.art_countries, "brands" to R.drawable.art_brands,
        "vehicles" to R.drawable.art_vehicles, "hobbies" to R.drawable.art_hobbies, "act" to R.drawable.art_act,
        "emotions" to R.drawable.art_emotions, "sounds" to R.drawable.art_sounds, "instruments" to R.drawable.art_instruments,
        "dance" to R.drawable.art_dance, "movies" to R.drawable.art_movies, "stars" to R.drawable.art_stars,
        "songs" to R.drawable.art_songs, "tv" to R.drawable.art_tv, "lines" to R.drawable.art_lines,
        "kids" to R.drawable.art_kids, "cartoons" to R.drawable.art_cartoons, "heroes" to R.drawable.art_heroes,
        "stories" to R.drawable.art_stories, "games" to R.drawable.art_games, "school" to R.drawable.art_school,
        "nature" to R.drawable.art_nature, "myths" to R.drawable.art_myths, "festivals" to R.drawable.art_festivals,
        "weddings" to R.drawable.art_weddings, "life" to R.drawable.art_life, "mix" to R.drawable.art_mix,
    )

    /** Art for a deck id such as "movies", "movies:us" or "mix:in". */
    @DrawableRes
    fun forId(id: String): Int? = byId[id.substringBefore(':')]
}

/** A deck's illustration, or its emoji for custom decks. */
@Composable
fun DeckIcon(id: String, emoji: String, size: Dp, modifier: Modifier = Modifier) {
    val res = DeckArt.forId(id)
    if (res != null) {
        Image(painterResource(res), contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = (size.value * 0.68f).sp)
        }
    }
}

/** Small 3D icon used on round buttons. */
@Composable
fun ArtIcon(@DrawableRes res: Int, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Image(painterResource(res), contentDescription = null, modifier = modifier.size(size))
