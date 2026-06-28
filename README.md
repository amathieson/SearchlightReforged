# Searchlight (& Wall Lights) (Unofficial NeoForge Port)
![Version](https://img.shields.io/badge/version-v1.0.0A-blue) ![NeoForge](https://img.shields.io/badge/NeoForge-21.1.65-orange)

## DISCLAIMER
This is a ported version of the Searchlight mod, originally created by Lizard-Of-Oz. The port is unofficial and not affiliated with the original mod or its creators.
Please do not report bugs to the original mod author. If you encounter any issues, please report them to this repository's issue tracker.

You can visit the original mod's CurseForge page at https://www.curseforge.com/minecraft/mc-mods/searchlight-forge or the GitHub repository https://github.com/Lizard-Of-Oz/Searchlight.

Please send credit to the original mod author, **Lizard-Of-Oz**, for creating the Searchlight mod. I merely ported and adapted it for NeoForge.

You can send donations to the original mod author via [Boosty](https://boosty.to/lizardofoz). This link is also available on the original mod's repository.

---

## Features & NeoForge Changes
For a full breakdown of the base features, blocks, and recipes, check out the original [SearchLight Repository](https://github.com/Lizard-Of-Oz/Searchlight).

### What's New in this Port:
- Fully ported to NeoForge and Minecraft 1.21.x
- Refactored codebase for better internal compatibility
- You can dim and brighten the lights by right-clicking with glowstone (to increase) or redstone (to decrease)
- Redstone toggleable (if provided with a redstone signal, light goes off)
- Computer Craft Integration
    - GetBrightness() 
    - SetBrightness(int)
  
### Todo List:
- [ ] Dyeable searchlights
- [ ] fix the super bright beacon 
- [ ] fix recipes
- [ ] DYENAMICS SUPPORT?

### Release Automation
Pushing a version tag matching `v*` (e.g., `v1.0.1`) automatically triggers a GitHub Actions workflow that:
- Bumps the `mod_version` in [`gradle.properties`](gradle.properties) to match the tag version (stripping the `v` prefix).
- Updates the version badge shown at the top of this [`README.md`](README.md).
- Builds the project and publishes a new GitHub Release with the compiled `.jar` build artifacts.

