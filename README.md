# IFetch

**IFetch** is a cross-platform network information tool that runs on the JVM (Java 21+). It outputs system and network info in multiple formats or serves it over HTTP. Whether used manually, in automation, or as an always-on API, IFetch is a compact and flexible utility that just works.

---

## 🔧 Features

- **External IP detection** (IPv4 & IPv6 via [ipify.org](https://ipify.org))
- **LAN IP and MAC address enumeration**
- **Full system info report**: OS, hostname, architecture, time, etc.
- **IPv6 availability check**
- **Output formats**: human-readable, JSON, YAML, CSV, XML
- **Minimal output mode** with `--simple`
- **Built-in HTTP API server**  
  - `/info`, `/info/json`, `/info/yaml`, `/info/xml`, etc.
  - Optional API key header + rate limiting

---

## 💾 Installation

### 📦 Prebuilt ZIP

1. Download the latest release zip from the [Releases](https://github.com/Uippao/IFetch/releases) page.
2. Extract it anywhere. It includes:
   - `ifetch.bat` – Windows launcher
   - `ifetch` – Linux/macOS launcher (chmod +x if needed)
   - `ifetch.jar` – The executable Java archive (requires Java 21+)

### 🧪 Dependencies

- **Java 21 or newer** must be installed and accessible in your `PATH`.  
  You can check with:

  ```bash
  java -version
````

If not installed, download it from [Adoptium](https://adoptium.net/) or use your system's package manager.

---

## 🚀 Usage

Use the included scripts to run IFetch:

```bash
./ifetch            # On Linux/macOS
ifetch.bat          # On Windows
```

You can also run it directly:

```bash
java -jar ifetch.jar [options]
```

### CLI Options

```text
Usage: ifetch [options]

Options:
  -h, --help           Show help text
  -s, --simple         Minimal: external IPs + one LAN IP + one MAC
  -j, --json           Output JSON
  -y, --yaml           Output YAML
  -c, --csv            Output CSV
  -x, --xml            Output XML
  --serve [port]       Start API server (default port 7676)
  --api-key [key]      Require X-API-Key header for all endpoints
  --rate-limit [n]     Max requests per IP per minute
```

---

## 🌐 API Server

IFetch can be run as a lightweight HTTP server to expose your system/network info over the network.

### Example:

```bash
./ifetch --serve 8080 --api-key secret123 --rate-limit 10
```

Then access:

| Endpoint       | Description                       | Content-Type     |
| -------------- | --------------------------------- | ---------------- |
| `/`            | Health check ("running")          | text/plain       |
| `/info`        | Full human-readable info          | text/plain       |
| `/info/simple` | Minimal version (like `--simple`) | text/plain       |
| `/info/json`   | JSON format                       | application/json |
| `/info/yaml`   | YAML format                       | text/plain       |
| `/info/csv`    | CSV format                        | text/csv         |
| `/info/xml`    | XML format                        | text/xml         |

#### API Key

* Enabled with `--api-key somekey`
* Required via the `X-API-Key` header in requests

#### Rate Limiting

* Enabled with `--rate-limit N`
* Blocks requests exceeding N per minute per IP
* Returns HTTP 429 on excess

---

## 🏗️ Building from Source

You can build IFetch yourself if you want to customize it or ensure compatibility.

### Requirements

* Java 21+
* Gradle (wrapper included)

### Build Instructions

```bash
git clone https://github.com/Uippao/IFetch.git
cd IFetch
./gradlew shadowJar
```

The fat jar will be located at:

```
build/libs/ifetch-1.0.0-all.jar
```

To run it:

```bash
java -jar build/libs/ifetch-1.0.0-all.jar [options]
```

---

## ✅ Example Output

### Simple mode

```bash
$ ./ifetch --simple
External IP (IPv4): 203.0.113.45
External IP (IPv6): Not configured
LAN IP: 192.168.1.42
MAC Address: 01:23:45:67:89:AB
```

### JSON mode

```bash
$ ./ifetch --json
{
  "timestamp": "2025-05-25T20:19:00Z",
  "hostname": "my-host",
  "os": {
    "name": "Linux",
    "version": "6.8.10",
    "arch": "x86_64"
  },
  "externalIPv4": "203.0.113.45",
  "externalIPv6": null,
  "ipv6Available": false,
  ...
}
```

---

## 🛠 Use Cases

* **Sysadmin diagnostics**: Check IPs and MACs quickly on any platform.
* **Automation scripts**: Parse JSON/YAML/CSV output.
* **Server dashboards**: Pull live data from IFetch’s local API.
* **Containers or VMs**: Run IFetch to report ephemeral network info.
* **Personal cloud devices**: Expose public IP securely via HTTP.

---

IFetch is a single-file, JVM-based solution that just works—on any machine, anywhere.
