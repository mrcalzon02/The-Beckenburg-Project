# Installed editor MCP repair

The Windows runner installed commit `179635d2643a89c888397e5dc497e692237339cd` into a disposable directory and launched `Beckenburg.exe --editor`. The test probe confirmed the executable, game JAR and Python adapter paths. [Installed application paths](installed-mcp.txt) · [Windows workflow](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/42474056244)

In an isolated Castle profile, the fixture placed a wall across the player spawn. An external MCP client observed the collision, removed the wall in a native transaction, and verified its stable ID in the committed change page. The spawn-wall finding went from one to zero. Eleven unrelated advisory findings remained. Human Undo restored the wall. [Native audit and change record](mcp-castle-repair.json)

The image is the installed game renderer's output after removal. [Castle repair capture](mcp-castle-repair.png)
