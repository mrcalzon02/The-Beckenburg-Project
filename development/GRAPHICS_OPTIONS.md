# Desktop display and PSX rendering

The installed desktop uses OpenGL 3.3 to render the world into an offscreen framebuffer,
then reads it back for the native-resolution Java 17 Swing HUD. A software
rasterizer remains as the explicit fallback; no VSync guarantee is implied.
GraphicsOptions is persisted in Java Preferences and entirely client-local.
Modern screen output modes: windowed, borderless window, borderless fullscreen,
and exclusive fullscreen at the current monitor resolution when AWT supports it.
Window size choices include 720p, 900p, 1080p, 1440p, 2160p and ultrawide.
Oversized windowed selections are safely clamped to available monitor space.
The Options dialog confirms mode switches and automatically reverts after
15 seconds if unconfirmed. F11 and Alt+Enter toggle fullscreen/windowed.

The render target is now a real resizable RGB and depth buffer. Resolution
quality sets internal height (200p/270p/360p/480p/540p/720p/1080p); actual width
follows the selected aspect ratio. Auto derives the monitor viewport aspect.
Forced 4:3, 16:9, 16:10, or 21:9 retain correct projection with letterbox or
pillarbox bands, not stretched sprites or walls. All solo, castle and LAN
first-person views use this configuration. Nameplate coordinates follow actual
framebuffer and letterboxed viewport positions. Van camera visuals retain
their own equipment-based optic zoom and monitor interpolation.

New preferences default to 540p with an initialized GPU, 360p in software;
previously saved retro selections are still respected. 1080p requires the GPU;
without it, the selected option automatically falls back to a safe 720p.
At unusual ultrawide aspect ratios the 1920-pixel render-width limit scales
both dimensions down equally to avoid distortion. Native monitor/window modes
remain independent of internal resolution. Castle HUD labels are drawn at native
screen resolution and scaled to physical viewport dimensions (rather than
magnifying tiny low-resolution glyphs). The shelf-label atlas alone uses linear
GPU texture filtering for more stable type; other textures retain nearest-neighbor
PSX styling. This cannot make tiny plaques legible across an entire room; the
existing aimed-exhibit HUD identification continues to serve as readable text.

Optional scene-only rendering effects include fast/strong CPU edge-aware
anti-aliasing, nearest/bilinear/bicubic upscaling, brightness, contrast,
scanlines, vignette, grain and chromatic aberration. They are applied
after the already integrated PSX vertex snapping, affine texture warping,
dither and palette treatment, but before native-resolution Swing HUD drawing.
Software 720p is expensive on CPU; recommend Balanced on low-power machines.
GPU 1080p still incurs offscreen color/depth readback and CPU effects; it is an
explicit quality option, not proof that the whole pipeline is GPU-resident.
Hardware MSAA, GPU postprocessing and guaranteed VSync are not available
without adding a GPU renderer backend, and are not misrepresented here.

GraphicsOptionsTest covers frame allocation, 4:3 and widescreen mapping,
unchanged optical field of view, effect opt-out and CPU safety bounds.
Run with build.sh or build.cmd. Real multi-monitor and exclusive-fullscreen
playthroughs must be accepted on physical target hardware.
