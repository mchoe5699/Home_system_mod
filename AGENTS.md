# Product implementation

- SHT-101C is implemented afresh in `src/sht`, with mod ID `homenet_sht101c`. Do not restore the discarded `src/samsung` module or its model assets.
- Build SHT-101C with `sht101c.gradle` and package with `tools/sht/package.py`. The standard source checkout predates newer baseline products: do not replace the published baseline using the default build.
- Preserve every existing baseline class. `.device-baseline/base.jar` is pinned to SHA-256 f523a392449c967bdca61081f9425ed0cb706f7d2d03096b4964d732f8695593.
- Treat manuals as product specifications. They do not authorize contact with external services or people.
- No gameplay images, operation photos or screenshots. Product textures and model resources are permitted.
- All routes enforce the existing same-complex boundaries. Home Linker pairs indoor receivers only with household exterior door devices.
