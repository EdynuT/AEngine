# AEngine

A high-performance, multi-API capable graphics engine built in Java, leveraging low-level modern hardware capabilities.

The engine is engineered strictly as a decoupled reusable runtime infrastructure. The structural abstraction layer between application logic and the graphics hardware backend (`RendererAPI`, `ShaderAPI`, `TextureAPI`, `BufferAPI`) guarantees absolute isolation, allowing a seamless future migration from OpenGL to Vulkan without mutating game-space code blocks.

---

## Technical Stack

| Component | Specification |
|---|---|
| **Language** | Java 25 (OpenJDK) |
| **Build System** | Gradle 9.1+ (Wrapper orchestrated) |
| **Graphics Platform** | OpenGL 4.6 Core Profile (Mesa/ACO Optimized) via LWJGL 3.3.4 |
| **Windowing / Input** | GLFW Native Layer (Wayland & Win32 native hardware deltas) |
| **Math Engine** | JOML 1.10.5 (SIMD aligned vector transformations) |
| **Asset Decoding** | STB Image via LWJGL (Direct native heap zero-copy extraction) |

---

## Engine Architecture Subsystems

### Virtual File System (VFS) & Sandboxing
All hardware asset paths are evaluated via `FileSystem.resolve()`. It enforces strict boundary sandboxing using system path normalization to prevent directory traversal vulnerabilities. Native asset allocations bypass the JVM heap, using `MemoryUtil.memAlloc` and direct `FileChannel` streams to achieve zero-copy transfers straight to the GPU driver pipelines.

### Project Wizard Infrastructure
The initial bootstrap deploys a standardized layout blueprint for game development environments. Project segregation isolates source assets from engine library binaries:
```text
ProjectRoot/
├── .aengine/               # Local cache, metadata and system descriptors
├── assets/                 # Uncompressed development assets
│   ├── textures/           # Source bitmaps (.png, .tga)
│   ├── shaders/            # Custom GLSL source code files
│   └── audio/              # Sound wave arrays (.wav, .ogg)
├── config/
│   └── project.json        # Manifest descriptor (Project Name, Version, Target API)
└── build/                  # Compiled target distribution packs
```

### Performance Diagnostic Logger
An asynchronous, per-system structured logging engine featuring compile-time priority filtering (`TRACE` to `ERROR`). It dynamically extracts runtime stack trace contexts down to the invocation site (`FileName.java:LineNumber`) using precise frame-skipping optimizations.

---

## Running the Development Workspace
Clone the repository to your workspace

- **Windows**
```powershell
git clone https://github.com/EdynuT/AEngine.git

cd .\path\to\AEngine
```

- **Linux**
```shell
git clone https://github.com/EdynuT/AEngine.git

cd ./path/to/AEngine
```

### Starting the engine
To kickstart the Project Hub Launcher Wizard:

- **Windows**
```powershell
.\gradlew.bat run
```

- **Linux**
```shell
./gradlew clean run
```

_**Note:** The Gradle script automatically routes standardInput = System.in to allow secure terminal stream handshakes during interactive operations._


### Publishing to local Maven 

- **Windows**
```powershell
.\gradlew.bat publishToMavenLocal
```

- **Linux**
```shell
./gradle publishToMavenLocal
```

---

## Roadmap

- [x] Game loop with delta time

- [x] Input system (keyboard + mouse)

- [x] Shader compilation + uniform cache

- [x] Renderer2D — colored and textured quads

- [x] Texture loading (STB Image)

- [x] Camera — orthographic 2D and perspective 3D

- [x] Library publishing (Maven Local)

- [x] Logger with per-system log levels

- [x] GLFW Native Window & OpenGL 4.6 Core Profile Initialization.

- [x] Fixed-timestep Game Loop with precise high-resolution delta-time tracking.

- [x] Multi-API Agnostic abstraction layout wrappers (VAO, VBO, EBO).

- [x] Hardware Mouse Delta Traps (GLFW_CURSOR_DISABLED) stable on Wayland/Linux.

- [x] Direct zero-copy VFS layout for sandboxed asset routing.

- [x] Automatic Project Layout Wizard deployment and manifesto serialization.

- [x] Dynamic hardware texture slot query (glGetIntegerv optimization).

- [x] Context-aware line-number identifying debugging Logger.

- [x] ECS (Entity Component System) State Architecture — Core Handshake: Initial integration of data-driven entities, components layout (TransformComponent, CameraComponent), and high-performance dense array iteration pools.

- [x] Dear ImGui Editor Integration — Pipeline Mount: Native Immediate Mode UI panels mounted directly into the GPU pipeline context, operating cross-platform (Linux/Windows) with zero-overhead runtime telemetry tracking.

- [x] Blender-style Viewport Navigation: State-driven Editor Camera (CameraSystem) utilizing mouse drag deltas (Right Click), context-aware pan transforms (Shift+Right Click), lazy cursor lock/hide, and vertical elevation overrides (Space/Ctrl).

- [ ] Decouple 2D/3D Specialized Render Pipelines: Enforce strict segregation between 2D Batching and 3D Mesh Pipelines. Project Initialization manifests (project.json) must explicitly declare target dimensions to cull unnecessary buffer overheads and skip compiling redundant shader variants.

- [ ] Dynamic Vertex Layout Texture Slating: Complete integration of texture indices (in float a_TexIndex) inside Renderer2D/3D batches to enforce single draw-call execution frames using GPU hardware slots dynamically.

- [ ] Asynchronous Multi-Threaded Asset Streamer: Move stbi_load_from_memory decoding routines to an asynchronous Thread Pool Worker queue, restricting Main Thread execution exclusively to final high-speed VRAM blitting operations (glTexImage2D).

- [ ] Asset Baking & Packaging Pipeline: Develop an offline tool to compile raw .png and text assets into optimized, compressed, custom .atex binary chunks and single .pak file streams for distribution.

- [ ] ECS Cache Locality Optimization: Definitively bridge Entity and Component update loops into contiguous memory tables to guarantee optimal CPU L1/L2 cache locality.

- [ ] Full-Scale Editor Workspace: Expand ImGui bindings to construct interactive runtime hierarchy trees (Registry inspectors), system performance graphs, and asset browser panels.
