# YouTube Lite for Windows 7 & Canaima (x86)

Ultra-lightweight, standalone desktop YouTube client engineered for low-spec legacy hardware running **Windows 7 SP1 32-bit**, with specific hardware tuning for **Intel Atom N455** netbooks (Canaima Magallanes / Letras Rojas / Letras Azules) and Canaima educational tablets (1GB - 2GB RAM).

## Key Features

- **Privacy-first & Zero Account Required:** Powered by [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) v0.26.5. Resolves videos, channels, and streams without Google API keys, telemetry, or login.
- **Ultra-Low Memory & CPU Footprint:** Configured with a Serial Garbage Collector and strict heap constraints (`-Xms16m -Xmx96m`), leaving precious system RAM free for the OS and video player.
- **Dedicated "Audio-Only" Mode (🎧):** Drops CPU consumption on single-core Atom processors from ~85% down to **<3%** by extracting Opus/AAC audio streams directly—ideal for listening to music, lectures, and podcasts with zero thermal throttling.
- **Canaima-Optimized Compact UI (1024 × 600 Viewport):**
  - Ultra-compact single-row header (38px height) saving over 120px of vertical working space compared to standard Swing layouts.
  - 120 × 68 thumbnail cards with embedded video duration badges.
  - Collapsible metadata bar and horizontal recent channel chips.
- **VLC Single-Instance Orchestrator:** Automatically terminates orphan background video processes to prevent memory thrashing on 1GB RAM systems; leverages native hardware decoding via 32-bit VLC with frame-drop protection (`--drop-late-frames --skip-frames`).
- **Resilient Fallback Pipeline:** Selects progressive MP4 streams up to 360p (sweet spot for GMA 3150), with automatic fallback to VLC direct stream resolution for DASH-only videos.

## Target Hardware Specifications

| Component | Minimum Specification | Recommended |
| :--- | :--- | :--- |
| **Processor** | Intel Atom N455 (1.66 GHz, 1C/2T) | Intel Celeron 847 / Atom Z3735F |
| **Memory (RAM)** | 1 GB DDR3 | 2 GB DDR3 |
| **Graphics** | Intel GMA 3150 / PowerVR SGX545 | Intel HD Graphics |
| **Display** | 1024 × 600 (Netbook) | 1280 × 800 (Tablet) / 1366 × 768 |
| **Operating System** | Windows 7 SP1 (32-bit x86) | Windows 7 SP1 / Windows 10 x86 |
| **Dependencies** | VLC Media Player (32-bit) | VLC Media Player 3.0+ x86 |

## Repository Structure

```
├── lib/                     # Dependency JARs (NewPipeExtractor, jsoup, rhino, etc.)
├── src/
│   └── main/
│       └── java/
│           └── net/newpipe/app/
│               ├── DesktopDownloader.java   # HTTP/HTTPS downloader with spoofed headers
│               └── Win7Main.java            # Optimized Swing GUI, player & cache engine
├── build.bat                # Direct JDK 21 build script
├── NewPipe-Windows-7.bat    # Canaima launcher with custom JVM tuning flags
└── README.md
```

## Building from Source

Ensure you have a working JDK (JDK 17 or JDK 21 x86/x64):

```bat
build.bat
```

This compiles all Java classes into `build/classes` and generates `lib/desktopApp.jar`.

## Launching

Ensure 32-bit VLC is installed, then run:

```bat
NewPipe-Windows-7.bat
```
