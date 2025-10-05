# App IPTV con Kotlin + ExoPlayer
## FastAPI + PostgreSQL + Android Nativo

### Resumen del Stack

**Backend:** FastAPI + PostgreSQL + Redis
**App Android:** Kotlin + ExoPlayer + Jetpack Compose
**Base de Datos:** PostgreSQL con optimizaciones para streaming
**Caché:** Redis para metadatos y listas M3U
**Autenticación:** JWT tokens

---

### Arquitectura del Sistema

#### Stack Backend
- **FastAPI** - API REST asíncrona de alta performance
- **PostgreSQL** - Base de datos principal
- **Redis** - Caché para streams y metadatos
- **SQLAlchemy** - ORM asíncrono
- **Alembic** - Migraciones de base de datos
- **Celery** - Tareas asíncronas (actualización EPG)
- **JWT** - Autenticación stateless

#### Stack Android
- **Kotlin** - Lenguaje nativo Android
- **ExoPlayer** - Reproductor multimedia de Google
- **Jetpack Compose** - UI moderna declarativa
- **Retrofit** - Cliente HTTP para API
- **Room** - Base de datos local (caché offline)
- **Hilt** - Inyección de dependencias
- **Coroutines** - Programación asíncrona

---

### Estructura del Proyecto

#### Backend (FastAPI)
```
iptv-backend/
├── app/
│   ├── __init__.py
│   ├── main.py               # FastAPI app
│   ├── core/
│   │   ├── config.py         # Configuración
│   │   ├── security.py       # JWT y auth
│   │   └── database.py       # PostgreSQL setup
│   ├── models/
│   │   ├── user.py           # Usuario SQLAlchemy
│   │   ├── channel.py        # Canal de TV
│   │   ├── category.py       # Categorías
│   │   ├── favorite.py       # Favoritos usuario
│   │   └── epg.py            # Guía programación
│   ├── schemas/
│   │   ├── user.py           # Pydantic schemas
│   │   ├── channel.py        # Validación datos
│   │   ├── auth.py           # Login/register
│   │   └── epg.py            # EPG responses
│   ├── api/
│   │   ├── auth.py           # Endpoints autenticación
│   │   ├── channels.py       # CRUD canales
│   │   ├── users.py          # Gestión usuarios
│   │   ├── favorites.py      # Favoritos
│   │   └── epg.py            # Guía programación
│   ├── services/
│   │   ├── auth_service.py   # Lógica autenticación
│   │   ├── channel_service.py # Lógica canales
│   │   ├── m3u_parser.py     # Parser listas M3U
│   │   └── epg_service.py    # Procesamiento EPG
│   ├── utils/
│   │   ├── helpers.py
│   │   └── validators.py
│   └── tasks/
│       ├── celery_app.py     # Configuración Celery
│       └── epg_tasks.py      # Tareas actualización EPG
├── migrations/               # Alembic migrations
├── requirements.txt
├── docker-compose.yml
└── Dockerfile
```

#### App Android (Kotlin)
```
iptv-android/
├── app/src/main/
│   ├── java/com/iptv/
│   │   ├── IPTVApplication.kt    # Application class
│   │   ├── di/
│   │   │   ├── DatabaseModule.kt # Hilt modules
│   │   │   ├── NetworkModule.kt  # Retrofit setup
│   │   │   └── PlayerModule.kt   # ExoPlayer setup
│   │   ├── data/
│   │   │   ├── api/
│   │   │   │   ├── IPTVApi.kt        # Retrofit interface
│   │   │   │   ├── AuthApi.kt        # Auth endpoints
│   │   │   │   └── dto/              # Data transfer objects
│   │   │   ├── database/
│   │   │   │   ├── IPTVDatabase.kt   # Room database
│   │   │   │   ├── entities/         # Room entities
│   │   │   │   └── dao/              # Data access objects
│   │   │   ├── repository/
│   │   │   │   ├── ChannelRepository.kt
│   │   │   │   ├── AuthRepository.kt
│   │   │   │   └── FavoriteRepository.kt
│   │   │   └── preferences/
│   │   │       └── UserPreferences.kt # SharedPreferences
│   │   ├── domain/
│   │   │   ├── model/            # Domain models
│   │   │   ├── usecase/          # Business logic
│   │   │   └── repository/       # Repository interfaces
│   │   ├── presentation/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/
│   │   │   │   ├── channels/
│   │   │   │   │   ├── ChannelsScreen.kt
│   │   │   │   │   └── ChannelsViewModel.kt
│   │   │   │   ├── player/
│   │   │   │   │   ├── PlayerScreen.kt
│   │   │   │   │   ├── PlayerViewModel.kt
│   │   │   │   │   └── ExoPlayerManager.kt
│   │   │   │   ├── favorites/
│   │   │   │   │   ├── FavoritesScreen.kt
│   │   │   │   │   └── FavoritesViewModel.kt
│   │   │   │   ├── auth/
│   │   │   │   │   ├── LoginScreen.kt
│   │   │   │   │   └── AuthViewModel.kt
│   │   │   │   └── components/    # Componentes reutilizables
│   │   │   └── theme/
│   │   │       ├── Color.kt
│   │   │       ├── Theme.kt
│   │   │       └── Type.kt
│   │   └── utils/
│   │       ├── Constants.kt
│   │       ├── Extensions.kt
│   │       └── NetworkUtils.kt
│   ├── res/
│   │   ├── layout/
│   │   ├── values/
│   │   ├── drawable/
│   │   └── mipmap/
│   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro
```

---

### Base de Datos PostgreSQL

#### Esquema Optimizado para IPTV

```sql
-- Usuarios del sistema
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    subscription_type VARCHAR(20) DEFAULT 'free', -- free, premium, vip
    subscription_expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Categorías de canales
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_url VARCHAR(500),
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true
);

-- Países
CREATE TABLE countries (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(3) UNIQUE NOT NULL, -- ISO code
    flag_url VARCHAR(500)
);

-- Canales de TV
CREATE TABLE channels (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    logo_url VARCHAR(500),
    stream_url VARCHAR(1000) NOT NULL,
    backup_stream_url VARCHAR(1000),
    category_id INTEGER REFERENCES categories(id),
    country_id INTEGER REFERENCES countries(id),
    language VARCHAR(10),
    quality VARCHAR(10) DEFAULT 'HD', -- SD, HD, FHD, 4K
    is_active BOOLEAN DEFAULT true,
    is_premium BOOLEAN DEFAULT false,
    sort_order INTEGER DEFAULT 0,
    view_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Favoritos por usuario
CREATE TABLE user_favorites (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    channel_id INTEGER REFERENCES channels(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, channel_id)
);

-- Historial de visualización
CREATE TABLE viewing_history (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    channel_id INTEGER REFERENCES channels(id) ON DELETE CASCADE,
    watched_at TIMESTAMP DEFAULT NOW(),
    watch_duration INTEGER DEFAULT 0, -- segundos
    device_info JSONB,
    ip_address INET
);

-- EPG (Electronic Program Guide)
CREATE TABLE epg_programs (
    id SERIAL PRIMARY KEY,
    channel_id INTEGER REFERENCES channels(id) ON DELETE CASCADE,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    duration INTEGER, -- minutos
    rating VARCHAR(10),
    poster_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Listas M3U importadas
CREATE TABLE m3u_playlists (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    url VARCHAR(1000),
    content TEXT, -- Contenido M3U completo
    last_updated TIMESTAMP DEFAULT NOW(),
    is_active BOOLEAN DEFAULT true,
    auto_update BOOLEAN DEFAULT false,
    update_interval INTEGER DEFAULT 24 -- horas
);

-- Índices para performance
CREATE INDEX idx_channels_category ON channels(category_id);
CREATE INDEX idx_channels_country ON channels(country_id);
CREATE INDEX idx_channels_active ON channels(is_active);
CREATE INDEX idx_user_favorites_user ON user_favorites(user_id);
CREATE INDEX idx_viewing_history_user ON viewing_history(user_id);
CREATE INDEX idx_viewing_history_watched ON viewing_history(watched_at);
CREATE INDEX idx_epg_channel_time ON epg_programs(channel_id, start_time, end_time);
```

---

### API Backend (FastAPI)

#### Endpoints Principales

```python
# main.py
from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.ext.asyncio import AsyncSession

app = FastAPI(
    title="IPTV API",
    description="API para aplicación IPTV",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Autenticación
@app.post("/auth/login", response_model=TokenResponse)
async def login(
    credentials: UserLogin,
    db: AsyncSession = Depends(get_db)
):
    user = await auth_service.authenticate_user(db, credentials)
    if not user:
        raise HTTPException(status_code=401, detail="Credenciales inválidas")
    
    access_token = create_access_token(data={"sub": user.username})
    return {"access_token": access_token, "token_type": "bearer"}

# Canales
@app.get("/channels", response_model=List[ChannelResponse])
async def get_channels(
    category_id: Optional[int] = None,
    country_id: Optional[int] = None,
    search: Optional[str] = None,
    skip: int = 0,
    limit: int = 100,
    db: AsyncSession = Depends(get_db)
):
    return await channel_service.get_channels(
        db, category_id, country_id, search, skip, limit
    )

@app.get("/channels/{channel_id}", response_model=ChannelDetailResponse)
async def get_channel(
    channel_id: int,
    db: AsyncSession = Depends(get_db)
):
    channel = await channel_service.get_channel_by_id(db, channel_id)
    if not channel:
        raise HTTPException(status_code=404, detail="Canal no encontrado")
    
    # Incrementar contador de visualizaciones
    await channel_service.increment_view_count(db, channel_id)
    return channel

# Categorías
@app.get("/categories", response_model=List[CategoryResponse])
async def get_categories(db: AsyncSession = Depends(get_db)):
    return await channel_service.get_categories(db)

# Favoritos
@app.get("/user/favorites", response_model=List[ChannelResponse])
async def get_user_favorites(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    return await favorite_service.get_user_favorites(db, current_user.id)

@app.post("/user/favorites/{channel_id}")
async def add_favorite(
    channel_id: int,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    await favorite_service.add_favorite(db, current_user.id, channel_id)
    return {"message": "Canal agregado a favoritos"}

@app.delete("/user/favorites/{channel_id}")
async def remove_favorite(
    channel_id: int,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    await favorite_service.remove_favorite(db, current_user.id, channel_id)
    return {"message": "Canal removido de favoritos"}

# EPG
@app.get("/epg/{channel_id}", response_model=List[EPGProgramResponse])
async def get_channel_epg(
    channel_id: int,
    date: Optional[str] = None,  # YYYY-MM-DD
    db: AsyncSession = Depends(get_db)
):
    return await epg_service.get_channel_programs(db, channel_id, date)

# Historial
@app.post("/user/history")
async def add_to_history(
    history_data: ViewingHistoryCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    await history_service.add_viewing_record(
        db, current_user.id, history_data
    )
    return {"message": "Agregado al historial"}

# M3U Import
@app.post("/admin/import-m3u")
async def import_m3u_playlist(
    m3u_data: M3UImportRequest,
    current_user: User = Depends(get_current_admin_user),
    db: AsyncSession = Depends(get_db)
):
    result = await m3u_service.import_playlist(db, m3u_data)
    return {"message": f"Importados {result.channels_added} canales"}
```

#### Servicios Backend

```python
# services/channel_service.py
class ChannelService:
    async def get_channels(
        self,
        db: AsyncSession,
        category_id: Optional[int] = None,
        country_id: Optional[int] = None,
        search: Optional[str] = None,
        skip: int = 0,
        limit: int = 100
    ) -> List[Channel]:
        query = select(Channel).where(Channel.is_active == True)
        
        if category_id:
            query = query.where(Channel.category_id == category_id)
        
        if country_id:
            query = query.where(Channel.country_id == country_id)
        
        if search:
            query = query.where(
                Channel.name.ilike(f"%{search}%")
            )
        
        query = query.offset(skip).limit(limit).order_by(Channel.sort_order)
        
        result = await db.execute(query)
        return result.scalars().all()

# services/m3u_parser.py
class M3UParser:
    def parse_m3u_content(self, content: str) -> List[ChannelData]:
        channels = []
        lines = content.strip().split('\n')
        
        i = 0
        while i < len(lines):
            if lines[i].startswith('#EXTINF:'):
                # Parse channel info
                info_line = lines[i]
                stream_url = lines[i + 1] if i + 1 < len(lines) else ""
                
                channel = self._parse_extinf_line(info_line, stream_url)
                if channel:
                    channels.append(channel)
                
                i += 2
            else:
                i += 1
        
        return channels
    
    def _parse_extinf_line(self, line: str, stream_url: str) -> Optional[ChannelData]:
        # Parse: #EXTINF:-1 tvg-id="channel1" tvg-logo="logo.png" group-title="Sports",Channel Name
        pattern = r'#EXTINF:(-?\d+)(?:\s+([^,]*))?,(.+)'
        match = re.match(pattern, line)
        
        if not match:
            return None
        
        duration, attributes, name = match.groups()
        
        # Parse attributes
        attrs = self._parse_attributes(attributes or "")
        
        return ChannelData(
            name=name.strip(),
            stream_url=stream_url.strip(),
            logo_url=attrs.get('tvg-logo', ''),
            group=attrs.get('group-title', 'General'),
            tvg_id=attrs.get('tvg-id', ''),
            language=attrs.get('tvg-language', ''),
            country=attrs.get('tvg-country', '')
        )
```

---

### App Android (Kotlin + ExoPlayer)

#### Configuración ExoPlayer

```kotlin
// player/ExoPlayerManager.kt
@Singleton
class ExoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null
    
    fun initializePlayer(): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context)
                .setLoadControl(buildLoadControl())
                .setRenderersFactory(buildRenderersFactory())
                .build()
        }
        return exoPlayer!!
    }
    
    private fun buildLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                1500, // Buffer para inicio rápido
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    private fun buildRenderersFactory(): RenderersFactory {
        return DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
    }
    
    fun playChannel(streamUrl: String) {
        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.02f)
                    .build()
            )
            .build()
        
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }
    
    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
```

#### UI con Jetpack Compose

```kotlin
// ui/channels/ChannelsScreen.kt
@Composable
fun ChannelsScreen(
    viewModel: ChannelsViewModel = hiltViewModel(),
    onChannelClick: (Channel) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Barra de búsqueda
        SearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        // Filtros por categoría
        CategoryTabs(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = viewModel::selectCategory,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Lista de canales
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                ErrorMessage(
                    message = uiState.error,
                    onRetry = viewModel::loadChannels,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.channels) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { onChannelClick(channel) },
                            onFavoriteClick = { viewModel.toggleFavorite(channel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Logo del canal
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(R.drawable.ic_tv_placeholder),
                error = painterResource(R.drawable.ic_tv_placeholder)
            )
            
            // Información del canal
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = channel.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (channel.isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = "Favorito",
                            tint = if (channel.isFavorite) {
                                Color.Red
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
```

#### Player Screen

```kotlin
// ui/player/PlayerScreen.kt
@Composable
fun PlayerScreen(
    channelId: Int,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(channelId) {
        viewModel.loadChannel(channelId)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // ExoPlayer View
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = viewModel.exoPlayer
                    useController = true
                    controllerAutoShow = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Overlay con información del canal
        uiState.currentChannel?.let { channel ->
            ChannelInfoOverlay(
                channel = channel,
                isVisible = uiState.showChannelInfo,
                onDismiss = { viewModel.hideChannelInfo() },
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
        
        // Controles personalizados
        PlayerControls(
            isPlaying = uiState.isPlaying,
            onPlayPause = viewModel::togglePlayPause,
            onPreviousChannel = viewModel::previousChannel,
            onNextChannel = viewModel::nextChannel,
            onShowChannelList = viewModel::showChannelList,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun PlayerControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onShowChannelList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color.Black.copy(alpha = 0.7f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousChannel) {
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = "Canal anterior",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        IconButton(onClick = onPlayPause) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        
        IconButton(onClick = onNextChannel) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = "Siguiente canal",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        IconButton(onClick = onShowChannelList) {
            Icon(
                Icons.Filled.List,
                contentDescription = "Lista de canales",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
```

---

### Configuración y Despliegue

#### Dependencias Backend
```txt
fastapi==0.104.1
uvicorn[standard]==0.24.0
sqlalchemy[asyncio]==2.0.23
asyncpg==0.29.0
alembic==1.12.1
redis==5.0.1
celery==5.3.4
python-jose[cryptography]==3.3.0
passlib[bcrypt]==1.7.4
python-multipart==0.0.6
pydantic==2.5.0
pydantic-settings==2.1.0
```

#### Dependencias Android
```kotlin
// build.gradle.kts (app)
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    kapt("com.google.dagger:hilt-compiler:2.48.1")
    
    // ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

#### Docker Compose
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: iptv_db
      POSTGRES_USER: iptv_user
      POSTGRES_PASSWORD: iptv_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  api:
    build: .
    ports:
      - "8000:8000"
    environment:
      - DATABASE_URL=postgresql+asyncpg://iptv_user:iptv_password@postgres:5432/iptv_db
      - REDIS_URL=redis://redis:6379
      - SECRET_KEY=your-secret-key-here
    depends_on:
      - postgres
      - redis
    volumes:
      - ./app:/app
    command: uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

  celery:
    build: .
    environment:
      - DATABASE_URL=postgresql+asyncpg://iptv_user:iptv_password@postgres:5432/iptv_db
      - REDIS_URL=redis://redis:6379
    depends_on:
      - postgres
      - redis
    volumes:
      - ./app:/app
    command: celery -A app.tasks.celery_app worker --loglevel=info

volumes:
  postgres_data:
```

---

### Funcionalidades Avanzadas

#### Caché con Redis

```python
# services/cache_service.py
import redis.asyncio as redis
import json
from typing import Optional, List

class CacheService:
    def __init__(self, redis_url: str):
        self.redis = redis.from_url(redis_url)
    
    async def get_channels_cache(self, cache_key: str) -> Optional[List[dict]]:
        cached = await self.redis.get(cache_key)
        if cached:
            return json.loads(cached)
        return None
    
    async def set_channels_cache(
        self, 
        cache_key: str, 
        channels: List[dict], 
        expire: int = 300
    ):
        await self.redis.setex(
            cache_key, 
            expire, 
            json.dumps(channels, default=str)
        )
    
    async def invalidate_channels_cache(self):
        keys = await self.redis.keys("channels:*")
        if keys:
            await self.redis.delete(*keys)
```

#### Offline Support Android

```kotlin
// data/repository/ChannelRepository.kt
@Singleton
class ChannelRepository @Inject constructor(
    private val api: IPTVApi,
    private val dao: ChannelDao,
    private val networkUtils: NetworkUtils
) {
    suspend fun getChannels(forceRefresh: Boolean = false): Flow<List<Channel>> {
        return flow {
            // Emitir datos locales primero
            emit(dao.getAllChannels())
            
            // Actualizar desde API si hay conexión
            if (networkUtils.isConnected() && (forceRefresh || shouldRefresh())) {
                try {
                    val remoteChannels = api.getChannels()
                    dao.insertChannels(remoteChannels)
                    emit(dao.getAllChannels())
                    updateLastRefreshTime()
                } catch (e: Exception) {
                    // Continuar con datos locales si falla la API
                    Timber.e(e, "Error fetching channels from API")
                }
            }
        }
    }
    
    private suspend fun shouldRefresh(): Boolean {
        val lastRefresh = preferences.getLastRefreshTime()
        val now = System.currentTimeMillis()
        return (now - lastRefresh) > REFRESH_INTERVAL
    }
    
    companion object {
        private const val REFRESH_INTERVAL = 30 * 60 * 1000L // 30 minutos
    }
}
```

---

### Performance y Optimizaciones

#### Base de Datos
- **Índices optimizados** para queries frecuentes
- **Connection pooling** con SQLAlchemy
- **Query optimization** con EXPLAIN ANALYZE
- **Partitioning** para tablas grandes (historial)

#### API
- **Caché Redis** para endpoints frecuentes
- **Paginación** en listas de canales
- **Compresión gzip** para responses
- **Rate limiting** para prevenir abuso

#### Android App
- **Image caching** con Coil
- **Database caching** con Room
- **Network caching** con OkHttp
- **Memory optimization** para listas grandes

#### LibVLC Player
- **Universal codec support** (mpga, AAC, AC-3, DTS, etc.)
- **Cross-platform compatibility** (Xiaomi, Samsung, TV Box)
- **HTTP/HTTPS streaming** sin restricciones
- **Hardware acceleration** automática
- **Advanced audio processing** para dispositivos problemáticos

---

### Seguridad

#### Backend
- **JWT tokens** con expiración
- **Password hashing** con bcrypt
- **CORS** configurado correctamente
- **Rate limiting** por IP
- **Input validation** con Pydantic
- **SQL injection** prevention con SQLAlchemy

#### Android
- **Certificate pinning** para API calls
- **Encrypted storage** para tokens
- **ProGuard** para ofuscación
- **Network security config**
- **Biometric authentication** opcional

---

### Roadmap de Implementación (Backend First Approach)

#### Fase 1: Backend Core (Semanas 1-2)
**Objetivo:** API funcional con datos básicos

**¿Por qué Backend Primero?**
- ✅ Validar que tus streams M3U funcionan correctamente
- ✅ Tener datos reales para probar desde el inicio
- ✅ API estable antes de desarrollar la UI Android
- ✅ Debugging más simple sin variables de UI
- ✅ Performance conocida antes de implementar cliente

**Plan de Implementación Detallado:**

**Día 1-2: Setup Base**
```bash
# 1. Crear proyecto FastAPI
mkdir iptv-backend && cd iptv-backend
python -m venv venv
source venv/bin/activate  # Linux/Mac
# venv\Scripts\activate  # Windows
pip install fastapi uvicorn sqlalchemy[asyncio] asyncpg alembic

# 2. PostgreSQL con Docker (recomendado)
docker run --name iptv-postgres \
  -e POSTGRES_DB=iptv_db \
  -e POSTGRES_USER=iptv_user \
  -e POSTGRES_PASSWORD=iptv_password \
  -p 5432:5432 -d postgres:15

# 3. Redis para caché
docker run --name iptv-redis -p 6379:6379 -d redis:7-alpine
```

**Día 3-4: Parser M3U + Base de Datos**
- [ ] Implementar parser M3U del documento
- [ ] Crear tablas básicas (channels, categories, countries)
- [ ] Endpoint `/admin/import-m3u` funcional
- [ ] Validar streams con requests HTTP
- [ ] Configurar Alembic para migraciones

**Día 5-7: API Básica**
- [ ] Endpoints `/channels` con filtros y paginación
- [ ] Endpoint `/categories` para filtros UI
- [ ] Sistema de autenticación JWT básico
- [ ] Middleware CORS para desarrollo
- [ ] Testing completo con Postman
- [ ] Documentación Swagger automática

**Checklist de Validación:**
- [ ] Parser M3U procesa tu lista correctamente
- [ ] Base de datos almacena canales sin errores
- [ ] API responde en < 200ms para listas de canales
- [ ] Streams válidos identificados correctamente
- [ ] Autenticación JWT funciona
- [ ] Documentación Swagger accesible en `/docs`

**Herramientas Necesarias:**
- **Editor:** VS Code/PyCharm + extensiones Python
- **Base de datos:** PostgreSQL (Docker recomendado)
- **Testing:** Postman o Thunder Client (VS Code)
- **Tu lista M3U** para validar parser
- **Docker Desktop** para containers

**Comandos de Testing Rápido:**
```bash
# Verificar conexión DB
psql -h localhost -U iptv_user -d iptv_db

# Ejecutar servidor desarrollo
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Testing endpoints
curl http://localhost:8000/channels
curl http://localhost:8000/docs  # Swagger UI
```

#### Fase 2: Backend Avanzado (Semana 3)
**Objetivo:** API completa y optimizada
- [ ] Sistema de caché con Redis
- [ ] Endpoints de favoritos y historial
- [ ] EPG processing básico
- [ ] Admin endpoints (CRUD canales)
- [ ] Rate limiting y seguridad
- [ ] Testing automatizado
- [ ] Performance optimization

**Entregable:** API REST completa y documentada

#### Fase 3: Android Setup (Semana 4)
**Objetivo:** Proyecto Android base funcionando
- [ ] Instalación y configuración Android Studio
- [ ] Setup proyecto con Kotlin + Hilt
- [ ] Configuración LibVLC básica
- [ ] Integración Retrofit con API (ya probada)
- [ ] UI básica con Jetpack Compose
- [ ] Testing de conectividad API

**Herramientas:** Android Studio + Emulador/Dispositivo físico

#### Fase 4: App Core Features (Semanas 5-6)
**Objetivo:** App funcional con features principales
- [ ] Lista de canales con categorías
- [ ] Reproductor LibVLC integrado
- [ ] Sistema de favoritos (sincronizado)
- [ ] Búsqueda y filtros
- [ ] Autenticación de usuarios
- [ ] Caché offline con Room
- [ ] Navigation entre pantallas

#### Fase 5: Features Avanzadas (Semana 7)
**Objetivo:** Funcionalidades premium
- [ ] EPG (guía de programación)
- [ ] Historial de visualización
- [ ] Configuraciones de usuario
- [ ] Picture-in-Picture mode
- [ ] Background playback
- [ ] Push notifications

#### Fase 6: Polish y Deploy (Semana 8)
**Objetivo:** App lista para producción
- [ ] UI/UX refinado y responsive
- [ ] Performance optimization
- [ ] Testing completo (unit + integration)
- [ ] Manejo de errores robusto
- [ ] Deployment backend (Docker)
- [ ] Build APK/AAB para distribución

---

### Ventajas del Backend First Approach

#### Desarrollo Más Eficiente
✅ **API estable** antes de desarrollar UI
✅ **Testing independiente** con herramientas simples
✅ **Validación de lógica** sin complejidad de UI
✅ **Documentación automática** con FastAPI
✅ **Debugging más fácil** sin variables de UI

#### Para IPTV Específicamente
✅ **Validar streams M3U** antes de implementar player
✅ **Parser optimizado** con datos reales
✅ **Base de datos** estructurada correctamente
✅ **Performance** del backend conocida
✅ **Caché strategy** definida desde inicio

#### Workflow Paralelo
✅ **Backend development** mientras Android Studio descarga
✅ **API testing** con Postman durante desarrollo
✅ **Database optimization** antes de app load
✅ **Security implementation** probada independientemente
✅ **Documentation** lista para equipo Android

#### Herramientas por Fase

**Backend Development:**
- **Kiro/VS Code/PyCharm** - Desarrollo Python
- **PostgreSQL + pgAdmin** - Base de datos
- **Redis** - Caché y sessions
- **Postman/Insomnia** - API testing
- **Docker** - Containerización

**Android Development:**
- **Android Studio** - IDE principal
- **Emulador/Device** - Testing
- **API ya funcionando** - Integración directa
- **Datos reales** - Testing con contenido real

---

### Ventajas del Stack

#### Técnicas
✅ **FastAPI**: Performance superior para APIs
✅ **LibVLC**: Reproductor universal con soporte completo de códecs
✅ **Kotlin**: Desarrollo nativo Android eficiente
✅ **PostgreSQL**: Robustez para datos relacionales
✅ **Jetpack Compose**: UI moderna y declarativa

#### Operacionales
✅ **Escalabilidad**: Arquitectura preparada para crecimiento
✅ **Mantenibilidad**: Código limpio y bien estructurado
✅ **Performance**: Optimizado para streaming en tiempo real
✅ **Offline support**: Funciona sin conexión a internet

#### Económicas
✅ **Open source**: Sin costos de licencias
✅ **Cloud ready**: Deploy en cualquier proveedor
✅ **Eficiente**: Uso optimizado de recursos
✅ **Escalamiento gradual**: Crece con la demanda

---

### Conclusión

Este stack proporciona una solución completa y robusta para una aplicación IPTV, combinando la performance de FastAPI y PostgreSQL en el backend con la eficiencia de Kotlin y LibVLC en Android. La arquitectura está diseñada para manejar miles de usuarios simultáneos con excelente calidad de streaming y compatibilidad universal de códecs, solucionando problemas de audio en dispositivos problemáticos como Xiaomi y TV Box chinos.