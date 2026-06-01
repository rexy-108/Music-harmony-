package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.PlaylistWithSongs
import com.example.data.Song
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlin.random.Random

// Navigation Sealed Hierarchy
sealed class AppScreen {
    object Library : AppScreen()
    object Playlists : AppScreen()
    object CommunityFeed : AppScreen()
    data class PlaylistDetails(val playlistWithSongs: PlaylistWithSongs) : AppScreen()
}

// Accent Colors for "Midnight Nebula"
val DarkBg = Color(0xFF0F0F16)
val CardGray = Color(0xFF1B1B26)
val SoftPurple = Color(0xFF503A8D)
val GlowNeonCyan = Color(0xFF00FFCC)
val CoralHot = Color(0xFFFF497C)
val NebulaTeal = Color(0xFF009688)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MusicViewModel) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Library) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val currentSong by viewModel.currentPlayingSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = "Logo",
                            tint = GlowNeonCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "MUSIC HARMONY",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 20.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                actions = {
                    if (currentScreen is AppScreen.PlaylistDetails) {
                        IconButton(onClick = { currentScreen = AppScreen.Playlists }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Go Back", tint = Color.LightGray)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg, titleContentColor = Color.White)
            )
        },
        bottomBar = {
            Column {
                // Expanded bottom player preview if any song is active
                currentSong?.let { song ->
                    MiniPlayerBar(
                        song = song,
                        isPlaying = isPlaying,
                        onPlayPauseToggle = { viewModel.togglePlayPause() },
                        onExpand = { isPlayerExpanded = true }
                    )
                }

                // Standard navigation items
                NavigationBar(
                    containerColor = Color(0xFF14141E),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Library,
                        onClick = { currentScreen = AppScreen.Library },
                        icon = { Icon(Icons.Filled.Explore, contentDescription = "Discover") },
                        label = { Text("Discover") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GlowNeonCyan,
                            selectedTextColor = GlowNeonCyan,
                            indicatorColor = SoftPurple,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("tab_discover")
                    )
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Playlists || currentScreen is AppScreen.PlaylistDetails,
                        onClick = { currentScreen = AppScreen.Playlists },
                        icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = "Playlists") },
                        label = { Text("Playlists") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GlowNeonCyan,
                            selectedTextColor = GlowNeonCyan,
                            indicatorColor = SoftPurple,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("tab_playlists")
                    )
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.CommunityFeed,
                        onClick = { currentScreen = AppScreen.CommunityFeed },
                        icon = { Icon(Icons.Filled.People, contentDescription = "Feed") },
                        label = { Text("Feed") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GlowNeonCyan,
                            selectedTextColor = GlowNeonCyan,
                            indicatorColor = SoftPurple,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("tab_feed")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen router animation
            AnimatedContent(
                targetState = currentScreen,
                label = "ScreenTransition",
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                }
            ) { screen ->
                when (screen) {
                    is AppScreen.Library -> LibraryTab(viewModel)
                    is AppScreen.Playlists -> PlaylistsTab(
                        viewModel = viewModel,
                        onOpenDetails = { currentScreen = AppScreen.PlaylistDetails(it) }
                    )
                    is AppScreen.CommunityFeed -> CommunityFeedTab(viewModel)
                    is AppScreen.PlaylistDetails -> PlaylistDetailScreen(
                        playlistWithSongs = screen.playlistWithSongs,
                        viewModel = viewModel,
                        onBack = { currentScreen = AppScreen.Playlists }
                    )
                }
            }

            // Expanded Full Custom Sheet Player
            if (isPlayerExpanded) {
                FullPlayerSheet(
                    viewModel = viewModel,
                    onDismiss = { isPlayerExpanded = false }
                )
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onExpand() }
            .testTag("mini_player"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spinning vinyl emulation inside miniplayer
            val infiniteTransition = rememberInfiniteTransition(label = "MiniVinyl")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "VinylSpinAngle"
            )

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .rotate(if (isPlaying) rotation else 0f)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = song.imageUrl.ifEmpty { "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300" },
                    contentDescription = "Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Center pin hole for record feeling
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(DarkBg, CircleShape)
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.singer,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onPlayPauseToggle,
                modifier = Modifier.testTag("mini_play_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = GlowNeonCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ---------------- Discover & Catalog ----------------
@Composable
fun LibraryTab(viewModel: MusicViewModel) {
    val songs by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val discoveryResult by viewModel.discoveryResult.collectAsState()

    var showManualAdd by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // AI query bar
        Text(
            text = "Global AI Lookup & Search",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search songs, artists, genres...", color = Color.Gray) },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("search_bar_input"),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray,
                    focusedContainerColor = CardGray,
                    unfocusedContainerColor = Color(0xFF14141E),
                    focusedIndicatorColor = GlowNeonCyan,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray) }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // AI crawler button
            Button(
                onClick = { viewModel.discoverWorldSong(searchQuery) },
                modifier = Modifier
                    .height(54.dp)
                    .testTag("ai_trigger_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = GlowNeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Import", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Crawling state overlay
        if (isDiscovering || discoveryResult != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1735)),
                border = BorderStroke(1.dp, GlowNeonCyan)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            color = GlowNeonCyan,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Done, contentDescription = null, tint = GlowNeonCyan)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = discoveryResult ?: "Analyzing music catalog...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Library header with Manual Add
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library Tracks (${songs.size})",
                color = GlowNeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            TextButton(
                onClick = { showManualAdd = true },
                modifier = Modifier.testTag("manual_add_trigger")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = CoralHot, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Custom Song", color = CoralHot, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tracks List
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.MusicOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No songs matching query.\nTry using 'AI Import' to query the world database!",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("songs_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    TrackRowItem(
                        song = song,
                        onClick = { viewModel.playSong(song, songs) }
                    )
                }
            }
        }
    }

    if (showManualAdd) {
        AddManualSongDialog(
            onDismiss = { showManualAdd = false },
            onConfirm = { title, singer, album, genre, duration, lyrics, pattern ->
                viewModel.addManualSong(title, singer, album, genre, duration, lyrics, pattern)
                showManualAdd = false
            }
        )
    }
}

@Composable
fun TrackRowItem(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag("song_row_${song.id}"),
        colors = CardDefaults.cardColors(containerColor = CardGray)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = song.imageUrl.ifEmpty { "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300" },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.singer,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(SoftPurple, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(song.genre, color = GlowNeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "${song.durationSeconds / 60}:${String.format("%02d", song.durationSeconds % 60)}",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = GlowNeonCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ---------------- Playlists ----------------
@Composable
fun PlaylistsTab(
    viewModel: MusicViewModel,
    onOpenDetails: (PlaylistWithSongs) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Custom Playlists",
                color = GlowNeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Row {
                IconButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.testTag("import_playlist_button")
                ) {
                    Icon(Icons.Filled.Input, contentDescription = "Import", tint = GlowNeonCyan)
                }

                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("create_playlist_button")
                ) {
                    Icon(Icons.Filled.AddBox, contentDescription = "Create", tint = CoralHot)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No custom playlists yet.\nCreate one or enter a friend's share code!",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("playlists_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(playlists) { element ->
                    PlaylistCardItem(
                        item = element,
                        onTap = { onOpenDetails(element) },
                        onShare = {
                            viewModel.sharePlaylist(context, element)
                            Toast.makeText(context, "Playlist Share Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { viewModel.deletePlaylist(element.playlist) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            songs = allSongs,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc, selected ->
                viewModel.createPlaylist(name, desc, selected)
                showCreateDialog = false
            }
        )
    }

    if (showImportDialog) {
        ImportPlaylistDialog(
            onDismiss = { showImportDialog = false },
            onImport = { code ->
                viewModel.importPlaylist(
                    code,
                    onSuccess = {
                        Toast.makeText(context, "Playlist imported successfully!", Toast.LENGTH_SHORT).show()
                        showImportDialog = false
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}

@Composable
fun PlaylistCardItem(
    item: PlaylistWithSongs,
    onTap: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onTap() }
            .testTag("playlist_card_${item.playlist.id}"),
        colors = CardDefaults.cardColors(containerColor = CardGray),
        border = BorderStroke(1.dp, Color(0xFF29293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.playlist.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Created by: ${item.playlist.creator}",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Row {
                    IconButton(onClick = onShare, modifier = Modifier.testTag("share_btn_${item.playlist.id}")) {
                        Icon(Icons.Filled.Share, contentDescription = "Share code", tint = GlowNeonCyan, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_btn_${item.playlist.id}")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = CoralHot, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.playlist.description.ifEmpty { "No description added." },
                color = Color.LightGray,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.songs.size} Track${if (item.songs.size != 1) "s" else ""}",
                    color = GlowNeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Listen Now", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = GlowNeonCyan, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ---------------- Playlist Details ----------------
@Composable
fun PlaylistDetailScreen(
    playlistWithSongs: PlaylistWithSongs,
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val activeDetail = playlists.firstOrNull { it.playlist.id == playlistWithSongs.playlist.id } ?: playlistWithSongs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = activeDetail.playlist.name,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                Text(
                    text = activeDetail.playlist.description,
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (activeDetail.songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No songs inside this playlist yet.", color = Color.Gray)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Playlist Tracks", color = GlowNeonCyan, fontWeight = FontWeight.Bold)

                Button(
                    onClick = { viewModel.playSong(activeDetail.songs.first(), activeDetail.songs) },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowNeonCyan)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("playlist_songs_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeDetail.songs) { song ->
                    TrackRowItem(
                        song = song,
                        onClick = { viewModel.playSong(song, activeDetail.songs) }
                    )
                }
            }
        }
    }
}

// ---------------- Community Feed Screen ----------------
@Composable
fun CommunityFeedTab(viewModel: MusicViewModel) {
    val context = LocalContext.current
    
    // Simulate real shared playlists from friends across the world
    val sampleFriendsPlaylists = remember {
        listOf(
            PlaylistWithSongs(
                playlist = com.example.data.Playlist(
                    id = 101,
                    name = "Retro Electropop Hits",
                    description = "Awesome synth waves and cosmic electrobeats shared by Rohit.",
                    creator = "Rohit Kushwaha"
                ),
                songs = listOf(
                    Song("hit_blinding_lights", "Blinding Lights", "The Weeknd", "After Hours", 200, "Synthwave")
                )
            ),
            PlaylistWithSongs(
                playlist = com.example.data.Playlist(
                    id = 102,
                    name = "Mellow Raindrops Piano",
                    description = "Soothing acoustic keys shared by Sara.",
                    creator = "Sara Jenkins"
                ),
                songs = listOf(
                    Song("hit_river_flows", "River Flows in You", "Yiruma", "First Love", 188, "Classical")
                )
            ),
            PlaylistWithSongs(
                playlist = com.example.data.Playlist(
                    id = 103,
                    name = "Vanguard Rock Anthems",
                    description = "Classic stadiums and hard rock shared by Alex.",
                    creator = "Alex Mercer"
                ),
                songs = listOf(
                    Song("hit_bohemian_rhapsody", "Bohemian Rhapsody", "Queen", "A Night", 354, "Rock")
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Collaborative Community Feed",
            color = GlowNeonCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = "Listen and import custom playlists shared by friends across the globe.",
            color = Color.LightGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sampleFriendsPlaylists) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141420)),
                    border = BorderStroke(1.dp, Color(0xFF1E1E2E))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Friends initials visual avatar
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(SoftPurple, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.playlist.creator.take(2).uppercase(),
                                        color = GlowNeonCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = item.playlist.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Shared by ${item.playlist.creator}",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Import simulated button
                            Button(
                                onClick = {
                                    val code = viewModel.sharePlaylist(context, item) // generate code
                                    val fullCode = "PLAYLIST_SHARE:${item.playlist.name}|${item.playlist.description}|${item.playlist.creator}|${item.songs.joinToString(",") { it.id }}"
                                    viewModel.importPlaylist(
                                        fullCode,
                                        onSuccess = {
                                            Toast.makeText(context, "Successfully Imported to your Library!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = {
                                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2F)),
                                border = BorderStroke(1.dp, GlowNeonCyan),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Download, contentDescription = null, tint = GlowNeonCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add to Mine", color = GlowNeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.playlist.description,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Listen track list
                        item.songs.forEach { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardGray, RoundedCornerShape(6.dp))
                                    .clickable { viewModel.playSong(song, item.songs) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = GlowNeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${song.title} - ${song.singer}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Expanded Player Sheet ----------------
@Composable
fun FullPlayerSheet(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val song by viewModel.currentPlayingSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()

    val context = LocalContext.current

    song?.let { activeSong ->
        // Generate floating particle animation values if playing
        val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "VinylSpinAngle"
        )

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp),
                color = DarkBg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF29293C))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        Text(
                            text = "NOW PLAYING",
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp
                        )

                        Box(
                            modifier = Modifier
                                .background(SoftPurple, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = activeSong.genre.uppercase(),
                                color = GlowNeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Vinyl record rotation with cover art in the center
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .rotate(if (isPlaying) rotation else 0f)
                            .background(Color.Black)
                            .border(6.dp, Color(0xFF2C2C3C), CircleShape)
                    ) {
                        // Glossy sound grooves drawing
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = size / 2f
                            drawCircle(color = Color.DarkGray, radius = (size.minDimension / 2f) * 0.8f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
                            drawCircle(color = Color.DarkGray, radius = (size.minDimension / 2f) * 0.6f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
                            drawCircle(color = Color.DarkGray, radius = (size.minDimension / 2f) * 0.4f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
                        }

                        AsyncImage(
                            model = activeSong.imageUrl.ifEmpty { "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300" },
                            contentDescription = null,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .align(Alignment.Center),
                            contentScale = ContentScale.Crop
                        )

                        // Center pin hole
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(DarkBg, CircleShape)
                                .align(Alignment.Center)
                                .border(2.dp, Color.LightGray, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = activeSong.title,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = activeSong.singer,
                        color = GlowNeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Album: ${activeSong.album}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Sound Frequency Visualizer!
                    AestheticSoundVisualizer(isPlaying = isPlaying, style = activeSong.synthPattern)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Slider
                    Slider(
                        value = progress.toFloat(),
                        onValueChange = { viewModel.seekTo(it.toInt()) },
                        valueRange = 0f..(activeSong.durationSeconds.toFloat()),
                        colors = SliderDefaults.colors(
                            thumbColor = GlowNeonCyan,
                            activeTrackColor = GlowNeonCyan,
                            inactiveTrackColor = Color.DarkGray
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${progress / 60}:${String.format("%02d", progress % 60)}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${activeSong.durationSeconds / 60}:${String.format("%02d", activeSong.durationSeconds % 60)}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    // Main Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.playPrevious() }) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(64.dp)
                                .background(GlowNeonCyan, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "PlayPause",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.playNext() }) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

                    // Audio Synthesizer preset info
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141420))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, tint = GlowNeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Synthesizer Preset: ${activeSong.synthPattern.replace("_", " ").uppercase()}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lyrics sheet
                    Text(
                        text = "SONG LYRICS & MELODY SHEET",
                        color = CoralHot,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B2B))
                    ) {
                        Text(
                            text = activeSong.lyrics.ifEmpty { "Hum along with the chiptune synthesizer loop!" },
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// Animated Audio Waves Visualizer
@Composable
fun AestheticSoundVisualizer(isPlaying: Boolean, style: String) {
    val barCount = 18
    val amplitudeList = remember { mutableStateListOf<Float>().apply { addAll(List(barCount) { 0.1f }) } }

    // Launch a ticking coroutine to cycle amplitude bars when playing
    LaunchedEffect(isPlaying, style) {
        if (isPlaying) {
            val speed = when (style) {
                "synthwave" -> 80L
                "ambient" -> 200L
                "lullaby" -> 250L
                else -> 120L
            }
            while (isActive) {
                for (i in 0 until barCount) {
                    amplitudeList[i] = Random.nextFloat().coerceIn(0.15f, 0.95f)
                }
                delay(speed)
            }
        } else {
            for (i in 0 until barCount) {
                amplitudeList[i] = 0.08f
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF14141E), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val animatedHeight by animateFloatAsState(
                targetValue = amplitudeList[i],
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "BarHeight"
            )
            val barColor = when (style) {
                "synthwave" -> CoralHot
                "ambient" -> SoftPurple
                "lullaby" -> GlowNeonCyan
                else -> GlowNeonCyan
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(animatedHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

// ---------------- DIALOGS ----------------
@Composable
fun AddManualSongDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, singer: String, album: String, genre: String, duration: Int, lyrics: String, synthStyle: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var singer by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Pop") }
    var durationText by remember { mutableStateOf("180") }
    var lyrics by remember { mutableStateOf("") }
    var selectedSynth by remember { mutableStateOf("pop_beat") }

    val genres = listOf("Pop", "Synthwave", "Ballad", "Classical", "Ambient", "Rock")
    val synths = listOf("pop_beat", "synthwave", "ambient", "ballad_chords", "lullaby")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            color = Color(0xFF1B1B26),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Custom Global Track",
                    color = GlowNeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title*", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("add_title")
                )

                OutlinedTextField(
                    value = singer,
                    onValueChange = { singer = it },
                    label = { Text("Singer / Artist*", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("add_singer")
                )

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album Name (Optional)", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Duration (seconds)*", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Genre:", color = Color.White, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    genres.forEach { g ->
                        FilterChip(
                            selected = genre == g,
                            onClick = { genre = g },
                            label = { Text(g) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GlowNeonCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Text("Chiptune Synth Style Preset:", color = Color.White, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    synths.forEach { s ->
                        FilterChip(
                            selected = selectedSynth == s,
                            onClick = { selectedSynth = s },
                            label = { Text(s.replace("_", " ")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it },
                    label = { Text("Song Lyrics / Notes Sheet", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && singer.isNotBlank()) {
                                val seconds = durationText.toIntOrNull() ?: 180
                                onConfirm(title, singer, album, genre, seconds, lyrics, selectedSynth)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlowNeonCyan),
                        modifier = Modifier.testTag("submit_manual_song")
                    ) {
                        Text("Add Track", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String, selectedSongs: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            color = Color(0xFF1B1B26),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Create Playlist",
                    color = GlowNeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name*", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("playlist_name_input")
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Short Description", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Add Songs to Playlist (${selectedIds.size})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color(0xFF14141E), RoundedCornerShape(8.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    songs.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (s.id in selectedIds) selectedIds.remove(s.id) else selectedIds.add(s.id)
                                }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = s.id in selectedIds,
                                onCheckedChange = {
                                    if (s.id in selectedIds) selectedIds.remove(s.id) else selectedIds.add(s.id)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = GlowNeonCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${s.title} - ${s.singer}", color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name, desc, selectedIds.toList())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlowNeonCyan),
                        modifier = Modifier.testTag("submit_create_playlist")
                    ) {
                        Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ImportPlaylistDialog(
    onDismiss: () -> Unit,
    onImport: (code: String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF1B1B26),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Import Shared Playlist",
                    color = GlowNeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Paste a friend's exported playlist code below to reconstruct their exact list instantly.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    placeholder = { Text("PLAYLIST_SHARE:...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .testTag("import_code_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onImport(code) },
                        colors = ButtonDefaults.buttonColors(containerColor = GlowNeonCyan),
                        modifier = Modifier.testTag("submit_import_playlist")
                    ) {
                        Text("Import List", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- Simple Horizontal Scroll Helper ----------------
@Composable
fun RowScope.FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    colors: SelectableChipColors
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = if (selected) colors.selectedContainerColor else Color(0xFF14141E),
        border = BorderStroke(1.dp, Color.Gray)
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            val labelColor = if (selected) colors.selectedLabelColor else Color.LightGray
            CompositionLocalProvider(LocalContentColor provides labelColor) {
                label()
            }
        }
    }
}

// Selectable Chip Color Holder
data class SelectableChipColors(
    val selectedContainerColor: Color,
    val selectedLabelColor: Color
)

object FilterChipDefaults {
    fun filterChipColors(selectedContainerColor: Color, selectedLabelColor: Color): SelectableChipColors {
        return SelectableChipColors(selectedContainerColor, selectedLabelColor)
    }
}
