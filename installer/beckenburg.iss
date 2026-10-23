#define AppVersion GetEnv("BECKENBURG_RELEASE_VERSION")
[Setup]
AppId={{E50BE280-83F3-45C0-8404-925A0E5B0D53}
AppName=Beckenburg
AppVersion={#AppVersion}
AppPublisher=The Beckenburg Project
DefaultDirName={autopf}\Beckenburg
DefaultGroupName=Beckenburg
OutputDir=..\dist\installer
OutputBaseFilename=Beckenburg-{#AppVersion}-Windows-x64
Compression=lzma
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=lowest
CloseApplications=yes
RestartApplications=no
UninstallDisplayIcon={app}\Beckenburg.exe
[Files]
Source: "..\dist\Beckenburg\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
[Icons]
Name: "{group}\Beckenburg"; Filename: "{app}\Beckenburg.exe"
Name: "{group}\Beckenburg (Software Renderer)"; Filename: "{app}\Beckenburg.exe"; Parameters: "--software"; WorkingDir: "{app}"
Name: "{group}\Beckenburg Level Editor"; Filename: "{app}\Beckenburg.exe"; Parameters: "--editor"; WorkingDir: "{app}"
Name: "{autodesktop}\Beckenburg"; Filename: "{app}\Beckenburg.exe"; Tasks: desktopicon
[Tasks]
Name: desktopicon; Description: "Create a desktop shortcut"; Flags: unchecked
[Run]
Filename: "{app}\Beckenburg.exe"; Description: "Launch Beckenburg"; Flags: nowait postinstall skipifsilent
