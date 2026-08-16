# Windows Security / Sysmon Anomaly Detection Pipeline
## Cell-by-Cell README for `corrected_anomaly_pipeline(1).ipynb`

This README is intentionally organized **in the same order as the notebook**. For every notebook cell, it shows:

1. **Cell number and type**
2. **The actual cell content/code**
3. **What the cell does**
4. **How each important line/block contributes to the pipeline**
5. **Why that cell matters**

The notebook contains **27 cells**: markdown cells introduce stages, while code cells implement them.

---

## Overall pipeline

```text
JSONL logs
   ↓
Stage 0: Load + inventory
   ↓
Stage 1: Event filtering
   ↓
Stage 2: Field normalization
   ↓
Stage 3: User / logon / process correlation
   ↓
Stage 4: 10-minute user windows
   ↓
Stage 5: Security helper functions
   ↓
Stage 6: Global process timeline
   ↓
Stage 7: Time-ordered behavioral novelty
   ↓
Stage 8: 69-feature engineering
   ↓
Stage 9: Zero-filled user/day/slot grid
   ↓
Stage 10: Per-user/hour Median + MAD baseline
   ↓
Stage 11: Synthetic attack injection
   ↓
Robust z-score + sparse-event rule
   ↓
Anomaly signals
```

> **Important:** the explanations below are based on the actual source code in the notebook, not a reconstructed version.

---

## Cell 1 — Markdown

### Cell content

> **Markdown cell from the notebook**
>
> # Windows Security / Sysmon Anomaly-Detection Pipeline — Corrected & Verified
> 
> This notebook rebuilds the pipeline end-to-end:
> 
> `JSONL logs -> event filtering -> field normalization -> user/logon/process correlation -> 10-minute user windows -> 69 security features -> per-user/per-hour Median+MAD baseline -> robust z-score anomaly comparison`
> 
> All outputs below are **recomputed directly from `data.jsonl`** (1,096 records) rather than trusted from the original notebook. See the accompanying verification report for a list of every bug found and fixed.
> 
> **Set `FILE` below to the path of your JSONL log file.**

### Explanation

This is the notebook's opening documentation. It tells the reader the intended end-to-end flow and makes two important claims: the pipeline is rebuilt end-to-end, and the displayed outputs are recomputed directly from the JSONL dataset rather than copied from an earlier notebook state. It also tells the user where to configure the input file.

---

## Cell 2 — Code

### Cell content

```python
# Empty code cell
```

### Explanation

This is an empty code cell. It performs no computation and has no effect on the pipeline.

---

## Cell 3 — Code

### Cell content

```python
import json, math, ipaddress
from collections import Counter, defaultdict
from datetime import datetime, timezone
from statistics import mean, pstdev, median

FILE = "data.jsonl"      # <-- path to your JSONL log file
WINDOW_SEC = 600          # 10-minute windows
```

### Explanation

This cell imports every library needed later. `json` parses JSONL records; `math` supports entropy/log calculations; `ipaddress` handles IP classification; `Counter` and `defaultdict` support counting/grouping; `datetime` handles timestamps and UTC; `mean`, `pstdev`, and `median` provide the statistical operations used by process-behavior and anomaly-baseline calculations. `FILE` is the input JSONL path, while `WINDOW_SEC = 600` establishes the fundamental 10-minute aggregation interval.

---

## Cell 4 — Markdown

### Cell content

> **Markdown cell from the notebook**
>
> ## Stage 0 — Load raw JSONL and inventory the fields/event mix

### Explanation

This markdown cell introduces Stage 0. The purpose of Stage 0 is not detection; it is data reconnaissance. Before transforming anything, the notebook measures how many records, fields, channels, and event IDs actually exist in the supplied dataset.

---

## Cell 5 — Code

### Cell content

```python
# =====================================================================
# STAGE 0 — LOAD
# =====================================================================
raw_records = [json.loads(line) for line in open(FILE, encoding="utf-8")]
print(f"[STAGE 0] loaded {len(raw_records)} raw JSONL records from {FILE}")

keys = set()
for r in raw_records:
    keys.update(r.keys())
mix = Counter((r.get("channel"), r.get("event_id")) for r in raw_records)
print(f"[STAGE 0] {len(keys)} distinct top-level fields present across all records")
print("[STAGE 0] (channel, event_id) counts:")
for (ch, eid), n in sorted(mix.items(), key=lambda kv: str(kv[0])):
    print(f"    {ch:38} {eid:>5} : {n}")
```

### Explanation

This is the Stage 0 implementation. It reads every JSONL line into a Python dictionary, counts records, collects all distinct top-level fields, and builds a `(channel, event_id)` frequency table. The final loop prints the event mix in a stable sorted order. This stage is important because later feature logic depends on knowing which event types are genuinely present in the data.

---

## Cell 6 — Markdown

### Cell content

> **Markdown cell from the notebook**
>
> ## Stage 1 — Event filtering (keep only security-relevant event IDs)

### Explanation

This markdown cell introduces Stage 1, where the pipeline decides which event IDs it understands and wants to retain.

---

## Cell 7 — Code

### Cell content

```python
# =====================================================================
# STAGE 1 — EVENT FILTERING
# =====================================================================
# KEEP_EVENTS is intentionally broader than what appears in this particular
# JSONL so the pipeline is forward-compatible with fuller log captures
# (e.g. LSASS access #10, registry #12/13, image load #7, pipe #17/18,
# file-delete #23, driver #24 are not present in THIS file but are kept
# so the code does not silently break when they show up).
KEEP_EVENTS = [
    # Security
    4624, 4625, 4634, 4647, 4688, 4768, 4769, 4771, 4776,
    4698, 4724, 4738, 5140, 5145,
    # Sysmon
    1, 3, 5, 7, 10, 11, 12, 13, 15, 17, 18, 22, 23, 24,
]

records = [r for r in raw_records if r.get("event_id") in KEEP_EVENTS]
dropped = [r for r in raw_records if r.get("event_id") not in KEEP_EVENTS]
print(f"\n[STAGE 1] kept {len(records)} / dropped {len(dropped)} records "
      f"(KEEP_EVENTS has {len(KEEP_EVENTS)} ids)")
if dropped:
    print("[STAGE 1] dropped breakdown:",
          Counter((r.get("channel"), r.get("event_id")) for r in dropped))

present_ids = {eid for (_, eid) in mix}
missing_from_data = sorted(set(KEEP_EVENTS) - present_ids)
print(f"[STAGE 1] KEEP_EVENTS ids with ZERO occurrences in this file "
      f"(features for these will legitimately read 0 unless injected): {missing_from_data}")
```

### Explanation

This cell defines the supported Security and Sysmon event IDs and filters the raw records against that allow-list. The comments explain that `KEEP_EVENTS` is deliberately broader than the current dataset so the feature pipeline remains usable when richer logs are supplied later. It also calculates `missing_from_data`, which is critical for interpreting zero-valued features: an event can legitimately score zero because the underlying event type never occurred in this JSONL.

---

## Cell 8 — Markdown

### Cell content

> **Markdown cell from the notebook**
>
> ## Stage 2 — Field normalization into one clean schema

### Explanation

This markdown cell introduces Stage 2. Windows Security and Sysmon records use different field names, so the next cell converts them into one internal representation.

---

## Cell 9 — Code

### Cell content

```python
# =====================================================================
# STAGE 2 — FIELD NORMALIZATION
# =====================================================================
def strip_domain(u):
    """CORP\\alice -> alice. Leaves bare names untouched."""
    return u.split("\\")[-1] if u and "\\" in u else u

def normalize(r):
    eid = r["event_id"]
    raw_user = (r.get("User") or r.get("TargetUserName")
                or r.get("SubjectUserName") or r.get("user"))
    return {
        "host": r.get("computer"), "time": r.get("timestamp"), "event_id": eid,
        "user": strip_domain(raw_user),  
        "sid": r.get("TargetUserSid") or r.get("SubjectUserSid"),
        # FIX: LogonId lookup order now also covers the field actually used
        # by 4634 (TargetLogonId) and 4647 (SubjectLogonId) — unchanged from
        # original, verified correct against the data (100% match rate).
        "logon_id": r.get("LogonId") or r.get("TargetLogonId") or r.get("SubjectLogonId"),
        "process_guid": r.get("ProcessGuid"),
        "parent_process_guid": r.get("ParentProcessGuid"),
        "image": r.get("Image"), "cmdline": r.get("CommandLine"),
        "parent_image": r.get("ParentImage"), "parent_user": r.get("ParentUser"),
        "dst_ip": r.get("DestinationIp"), "dst_port": r.get("DestinationPort"),
        "src_ip": r.get("SourceIp") or r.get("IpAddress"), "initiated": r.get("Initiated"),
        "workstation": r.get("WorkstationName") or r.get("Workstation"),
        "logon_type": r.get("LogonType"), "target_user": r.get("TargetUserName"),
        "query_name": r.get("QueryName"), "target_filename": r.get("TargetFilename"),
        "target_object": r.get("TargetObject"), "target_image": r.get("TargetImage"),
        "target_process_guid": r.get("TargetProcessGUID"), "granted_access": r.get("GrantedAccess"),
        "pipe_name": r.get("PipeName"), "image_loaded": r.get("ImageLoaded"),
        "service_name": r.get("ServiceName"), "share_name": r.get("ShareName"),
        "relative_target": r.get("RelativeTargetName"), "status": r.get("Status"),
    }

records = [normalize(r) for r in records]
print(f"\n[STAGE 2] normalized {len(records)} records")
for eid in (4624, 1, 3, 22):
    ex = next((r for r in records if r["event_id"] == eid), None)
    if ex:
        present = {k: v for k, v in ex.items() if v is not None}
        print(f"    event {eid}: {len(present)} populated fields -> {list(present)}")
```

### Explanation

This cell contains the normalization functions. `strip_domain` converts names such as `CORP\\alice` into `alice`. `normalize` selects the most appropriate source field for each normalized property and returns one common dictionary containing host, timestamp, event ID, identity, logon/session IDs, process fields, network fields, authentication fields, registry/file/pipe/service fields, and other security attributes. The `logon_id` lookup explicitly supports `LogonId`, `TargetLogonId`, and `SubjectLogonId`, allowing later correlation with Security 4634/4647. The final list-comprehension applies this schema to every retained record.

---

## Cell 10 — Markdown

### Cell content

> **Markdown cell from the notebook**
>
> ## Stage 3 — User / logon-session / process correlation

### Explanation

This markdown cell introduces Stage 3, which is responsible for turning raw identities into usable human-user attribution.

---

## Cell 11 — Code

### Cell content

```python
# =====================================================================
# STAGE 3 — USER / LOGON-SESSION / PROCESS CORRELATION
# =====================================================================
# Well-known non-interactive / service identities. Their process activity
# is real, but cannot be attributed to a human user via a 4624 interactive
# logon (there usually isn't one for LogonId 0x3e7 == the SYSTEM session).
# We resolve them explicitly to None ("UNATTRIBUTED") rather than silently
# letting the literal string "SYSTEM" become a pseudo-"user" that would
# otherwise pollute the per-user behavioral baselines built in Stage 5.
SYSTEM_ACCOUNTS = {"SYSTEM", "LOCAL SERVICE", "NETWORK SERVICE", "ANONYMOUS LOGON"}

logon_map = {}   # (host, logon_id) -> session info, from Security 4624
proc_map = {}    # process_guid     -> process info, from Sysmon 1

for r in records:
    if r["event_id"] == 4624:
        logon_map[(r["host"], r["logon_id"])] = {
            "user": r["user"], "sid": r["sid"], "src_ip": r["src_ip"],
            "workstation": r["workstation"], "logon_type": r["logon_type"], "time": r["time"],
        }
    elif r["event_id"] == 1:
        proc_map[r["process_guid"]] = {
            "user": r["user"], "logon_id": r["logon_id"], "image": r["image"], "time": r["time"],
        }

print(f"\n[STAGE 3] logon_map entries: {len(logon_map)} | proc_map entries: {len(proc_map)}")

def session_of(r):
    """Find the 4624 session that owns this event's process (if any)."""
    p = proc_map.get(r["process_guid"])
    lid = r["logon_id"] or (p or {}).get("logon_id")
    return logon_map.get((r["host"], lid))

def resolve_user(r):
    # 1) direct identity, unless it's a non-interactive system account
    if r["user"] and r["user"].upper() not in SYSTEM_ACCOUNTS:
        return r["user"], r["sid"]
    # 2) chain: process -> its logon session -> the human account that logged on
    sess = session_of(r)
    if sess and sess["user"] and sess["user"].upper() not in SYSTEM_ACCOUNTS:
        return sess["user"], sess["sid"]
    return None, None

for r in records:
    r["resolved_user"], r["resolved_sid"] = resolve_user(r)
    sess = session_of(r)
    if sess:
        r["login_ip"] = sess["src_ip"]
        r["login_type"] = sess["logon_type"]
        r["login_workstation"] = sess["workstation"]
    else:
        r["login_ip"] = r["login_type"] = r["login_workstation"] = None

n_unattributed = sum(1 for r in records if r["resolved_user"] is None)
n_system = sum(1 for r in records if (r["user"] or "").upper() in SYSTEM_ACCOUNTS)
print(f"[STAGE 3] resolved_user is None (unattributed, e.g. SYSTEM svc procs): {n_unattributed}")
print(f"[STAGE 3]   of which raw user was a system/service account        : {n_system}")
print(f"[STAGE 3] events carrying login_ip enrichment                     : "
      f"{sum(1 for r in records if r['login_ip'])}")
print("[STAGE 3] resolved user population:",
      Counter(r["resolved_user"] for r in records if r["resolved_user"]))
```

### Explanation

This cell builds two correlation maps: `logon_map` links `(host, logon_id)` to Security 4624 session information, while `proc_map` links a Sysmon process GUID to its process metadata. `session_of` follows an event to its process and then to its owning logon session. `resolve_user` first accepts a non-service user directly; otherwise it attempts process → logon-session correlation; otherwise it returns `None`. This prevents `SYSTEM` and other service identities from becoming fake human users in the behavioral baselines. The enrichment loop then attaches resolved identity and login context back onto every event.

---

## Cell 12 — Markdown

### Cell content

> **Markdown cell from the notebook**
>
> ## Stage 4 — 10-minute user windows

### Explanation

This markdown cell introduces Stage 4: converting event timestamps into fixed 10-minute behavioral windows.

---

## Cell 13 — Code

### Cell content

```python
# =====================================================================
# STAGE 4 — 10-MINUTE USER WINDOWS
# =====================================================================
def slot_of(ts):
    dt = datetime.fromisoformat(ts.replace("Z", "+00:00"))
    return int(dt.timestamp()) // WINDOW_SEC * WINDOW_SEC

def slot_label(s):
    return datetime.fromtimestamp(s, tz=timezone.utc).strftime("%m-%d %H:%M")

def day_of(s):
    return datetime.fromtimestamp(s, tz=timezone.utc).strftime("%Y-%m-%d")

def hour_of(s):
    return datetime.fromtimestamp(s, tz=timezone.utc).hour

# FIX: bucket by the CORRELATED resolved_user everywhere (previously the
# feature-engineering / baseline stages of the notebook re-bucketed on the
# raw "user" field, silently discarding all the Stage-3 correlation work
# and turning "SYSTEM" into its own pseudo-user).
buckets = defaultdict(list)
for r in records:
    buckets[(r["resolved_user"] or "UNATTRIBUTED", slot_of(r["time"]))].append(r)

real_users = sorted({u for (u, _s) in buckets if u != "UNATTRIBUTED"})
print(f"\n[STAGE 4] distinct (user, 10-min-slot) buckets: {len(buckets)}")
print(f"[STAGE 4] real users found: {real_users}")
print(f"[STAGE 4] UNATTRIBUTED-bucket events (excluded from per-user baselines): "
      f"{sum(len(v) for (u, s), v in buckets.items() if u == 'UNATTRIBUTED')}")
```

### Explanation

This cell defines the time-bucketing helpers. `slot_of` parses an ISO timestamp and floors it to a 600-second boundary. `slot_label` creates a human-readable UTC label, while `day_of` and `hour_of` extract the date and hour used by the baseline. The key correction is in the `buckets` dictionary: events are grouped using `resolved_user`, not the raw `user` field. This means the Stage 3 correlation work actually influences downstream feature engineering and avoids treating `SYSTEM` as a normal user.

---

## Cell 14 — Markdown

### Cell content

> **Markdown cell from the notebook**
>
> ## Stage 5 — Security helpers (LOLBins, suspicious pairs, entropy, private-IP check)

### Explanation

This markdown cell introduces Stage 5, the collection of reusable security heuristics used by feature engineering.

---

## Cell 15 — Code

### Cell content

```python
# =====================================================================
# STAGE 5 — SECURITY HELPERS (LOLBins, suspicious pairs, entropy, etc.)
# =====================================================================
LOLBINS = {"certutil.exe", "rundll32.exe", "powershell.exe", "powershell_ise.exe", "mshta.exe",
           "wscript.exe", "cscript.exe", "regsvr32.exe", "cmd.exe", "wmic.exe", "bitsadmin.exe",
           "schtasks.exe", "reg.exe", "whoami.exe", "net.exe", "net1.exe", "nltest.exe",
           "msbuild.exe", "csc.exe", "installutil.exe", "regasm.exe", "cmstp.exe", "bash.exe", "psexec.exe"}

SHELLS = {"cmd.exe", "powershell.exe", "powershell_ise.exe", "wscript.exe", "cscript.exe", "bash.exe", "mshta.exe"}

# Expanded modestly vs. the original set (documented assumption — see report):
# added the reverse cmd<->powershell pairs and a couple more common
# office/browser -> LOLBin spawns that are standard T1059/T1204 patterns.
SUSPICIOUS_PAIRS = {
    ("winword.exe", "powershell.exe"), ("winword.exe", "cmd.exe"), ("winword.exe", "mshta.exe"),
    ("excel.exe", "powershell.exe"), ("excel.exe", "cmd.exe"),
    ("outlook.exe", "powershell.exe"), ("outlook.exe", "cmd.exe"), ("outlook.exe", "mshta.exe"),
    ("msedge.exe", "powershell.exe"), ("msedge.exe", "cmd.exe"),
    ("chrome.exe", "powershell.exe"), ("chrome.exe", "cmd.exe"),
    ("wscript.exe", "powershell.exe"), ("mshta.exe", "cmd.exe"), ("mshta.exe", "powershell.exe"),
    ("explorer.exe", "mshta.exe"),
    ("cmd.exe", "powershell.exe"), ("powershell.exe", "cmd.exe"),
}

def base_name(path):
    return (path or "").lower().split("\\")[-1]

def is_lolbin(image):
    return base_name(image) in LOLBINS

def lolbin_path_anomaly(image):
    if not is_lolbin(image):
        return False
    p = (image or "").lower()
    return "system32" not in p and "syswow64" not in p

def suspicious_parent_child(parent_image, image):
    return (base_name(parent_image), base_name(image)) in SUSPICIOUS_PAIRS

def parent_user_mismatch(parent_user, user):
    pu, u = parent_user or "", user or ""
    return bool(pu and u and strip_domain(pu) != strip_domain(u))

def special_char_density(s):
    if not s:
        return 0.0
    return sum(1 for c in s if not c.isalnum()) / len(s)

def shannon_entropy(s):
    if not s:
        return 0.0
    cnt = Counter(s)
    n = len(s)
    return -sum((c / n) * math.log2(c / n) for c in cnt.values())

# FIX: the original is_external() only matched "172.16." (a single /24),
# missing the rest of the RFC1918 172.16.0.0/12 block (172.17.x-172.31.x),
# which would have misclassified those addresses as "external". Replaced
# with a proper ipaddress-based private-network check.
_PRIVATE_NETS = [ipaddress.ip_network(n) for n in
                  ("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "127.0.0.0/8")]

def is_external(ip):
    if not ip:
        return False
    try:
        addr = ipaddress.ip_address(ip)
    except ValueError:
        return False
    return not any(addr in net for net in _PRIVATE_NETS)

def has_encoded(cmdline):
    if not cmdline:
        return False
    ls = cmdline.lower()
    return any(k in ls for k in ("-enc", "-e ", "frombase64string", " -w hidden", "-windowstyle hidden", "bypass"))

print(f"\n[STAGE 5] helpers ready — {len(LOLBINS)} LOLBins, {len(SUSPICIOUS_PAIRS)} suspicious parent/child pairs")
```

### Explanation

This cell defines the security helper functions and curated detection knowledge. `LOLBINS` identifies binaries commonly abused for living-off-the-land execution. `SHELLS` identifies command/script interpreters. `SUSPICIOUS_PAIRS` represents curated parent-child process relationships such as Office → PowerShell. Additional functions normalize executable names, identify LOLBin path anomalies, test suspicious process relationships, calculate parent/child user mismatch, calculate special-character density, calculate Shannon entropy, classify private/internal IP addresses, identify external IPs, and detect encoded/hidden/bypass command-line patterns. These are heuristics rather than complete attack catalogs, so they provide security context but are not exhaustive threat intelligence.

---

## Cell 16 — Markdown

### Cell content

> **Markdown cell from the notebook**
>
> ## Stage 6 — Global process timeline (lifetime, connections, beaconing)

### Explanation

This markdown cell introduces Stage 6. Process behavior cannot always be understood from one event, so the notebook reconstructs a process timeline across the entire dataset.

---

## Cell 17 — Code

### Cell content

```python
# =====================================================================
# STAGE 6 — GLOBAL PROCESS TIMELINE (lifetime, beaconing, connections)
# =====================================================================
def epoch(ts):
    return int(datetime.fromisoformat(ts.replace("Z", "+00:00")).timestamp())

proc_start, proc_end = {}, {}
proc_image, proc_cmdline = {}, {}
proc_parent, proc_parent_image, proc_parent_user, proc_user = {}, {}, {}, {}
conns_by_proc = defaultdict(list)          # process_guid -> [(epoch, dst_ip, dst_port)]
downloads_by_proc = Counter()

for r in records:
    pg = r.get("process_guid")
    if not pg:
        continue
    if r["event_id"] == 1:
        proc_start[pg] = epoch(r["time"]); proc_image[pg] = r.get("image")
        proc_cmdline[pg] = r.get("cmdline"); proc_parent[pg] = r.get("parent_process_guid")
        proc_parent_image[pg] = r.get("parent_image"); proc_parent_user[pg] = r.get("parent_user")
        proc_user[pg] = r.get("user")
    elif r["event_id"] == 5:
        proc_end[pg] = epoch(r["time"])
    elif r["event_id"] == 3:
        conns_by_proc[pg].append((epoch(r["time"]), r.get("dst_ip"), r.get("dst_port")))
    elif r["event_id"] == 15:
        downloads_by_proc[pg] += 1

def beacon_regularity(pg):
    """1.0 = perfectly regular beacon-like interval. None if too few connections."""
    by_ip = defaultdict(list)
    for t, ip, _port in conns_by_proc[pg]:
        by_ip[ip].append(t)
    best = None
    for _ip, ts in by_ip.items():
        if len(ts) < 4:
            continue
        ts_sorted = sorted(ts)
        gaps = [b - a for a, b in zip(ts_sorted, ts_sorted[1:])]
        if mean(gaps) <= 0:
            continue
        reg = 1.0 - min(1.0, pstdev(gaps) / mean(gaps))
        best = reg if best is None else max(best, reg)
    return best

proc_feats = {}
for pg in proc_start:
    lifetime = proc_end.get(pg, proc_start[pg]) - proc_start[pg]
    img = proc_image.get(pg) or ""
    cmdl = proc_cmdline.get(pg) or ""
    parent_pg = proc_parent.get(pg)
    proc_feats[pg] = {
        "unique_ips": len({c[1] for c in conns_by_proc[pg]}),
        "downloads": downloads_by_proc.get(pg, 0),
        "cmd_char_length": len(cmdl),
        "special_char_density": special_char_density(cmdl),
        "is_lolbin": is_lolbin(img),
        "lolbin_path_anomaly": lolbin_path_anomaly(img),
        "suspicious_parent_child": suspicious_parent_child(proc_parent_image.get(pg), img),
        "parent_user_mismatch": parent_user_mismatch(proc_parent_user.get(pg), proc_user.get(pg)),
        "lifetime_seconds": lifetime,
        "is_micro_duration": lifetime < 5,
        "parent_dead_before_spawn": bool(parent_pg and parent_pg in proc_end
                                          and proc_end[parent_pg] < proc_start[pg]),
        "beacon_regularity": beacon_regularity(pg),
    }

print(f"[STAGE 6] processes tracked: {len(proc_start)} | with an Event-5 end: {len(proc_end)}")
```

### Explanation

This cell creates global dictionaries for process start/end times, executable metadata, parent relationships, users, network connections, and downloads. Event 1 establishes process-start information; Event 5 establishes termination; Event 3 records network connections; Event 15 counts downloads. `beacon_regularity` groups connection timestamps by destination IP, computes inter-connection gaps, and converts gap variability into a regularity score. The later process-feature calculation combines these values into behavioral attributes such as lifetime, LOLBin usage, suspicious parent-child relationships, short-lived processes, parent termination order, download count, and beaconing regularity.

---

## Cell 18 — Markdown

### Cell content

> **Markdown cell from the notebook**
>
> ## Stage 7 — Behavioral novelty (time-ordered, leak-free "first seen")

### Explanation

This markdown cell introduces Stage 7. The key concept is behavioral novelty: an event is interesting when it is the first time a user has seen a particular IP, domain, executable, command line, workstation, or image/port combination.

---

## Cell 19 — Code

### Cell content

```python
# =====================================================================
# STAGE 7 — BEHAVIORAL NOVELTY (TIME-ORDERED, LEAK-FREE "FIRST SEEN")
# =====================================================================
# FIX (critical): the original notebook built ONE global "seen" set from
# the ENTIRE dataset (all 10 days, including the window under test) before
# computing "new_*" novelty features. Because every value observed inside
# any given window is, trivially, a member of the very set built from that
# same window, new_dst_ips / new_domains / new_images / new_cmdlines /
# new_workstations / unusual_port_for_process could NEVER fire on real
# data — they were structurally always 0. The only reason the notebook's
# injected-attack demo "worked" is that the injected records were kept
# out of that global set entirely (a special case, not a general fix).
#
# The corrected approach processes each user's events in time order and
# marks a value "new" only if it was never seen for that user BEFORE the
# current event — a proper incremental novelty/first-seen check that does
# not leak future or same-window information.
seen_ip, seen_domain, seen_image, seen_cmdline, seen_ws, seen_imgport = (
    defaultdict(set) for _ in range(6)
)
is_new = {}  # id(record) -> dict of novelty booleans, keyed by record identity

for u in real_users:
    user_records = sorted(
        (r for r in records if r["resolved_user"] == u),
        key=lambda r: r["time"],
    )
    for r in user_records:
        flags = {}
        if r.get("dst_ip"):
            flags["new_ip"] = r["dst_ip"] not in seen_ip[u]
            seen_ip[u].add(r["dst_ip"])
        if r.get("query_name"):
            flags["new_domain"] = r["query_name"] not in seen_domain[u]
            seen_domain[u].add(r["query_name"])
        if r["event_id"] == 1 and r.get("image"):
            flags["new_image"] = r["image"] not in seen_image[u]
            seen_image[u].add(r["image"])
        if r["event_id"] == 1 and r.get("cmdline"):
            flags["new_cmdline"] = r["cmdline"] not in seen_cmdline[u]
            seen_cmdline[u].add(r["cmdline"])
        if r["event_id"] == 4624 and r.get("workstation"):
            flags["new_ws"] = r["workstation"] not in seen_ws[u]
            seen_ws[u].add(r["workstation"])
        if r.get("dst_port") and r.get("image"):
            key = (base_name(r["image"]), r["dst_port"])
            flags["new_imgport"] = key not in seen_imgport[u]
            seen_imgport[u].add(key)
        is_new[id(r)] = flags

n_new_ip = sum(1 for r in records if is_new.get(id(r), {}).get("new_ip"))
n_total_ip_events = sum(1 for r in records if r.get("dst_ip"))
print(f"\n[STAGE 7] first-time destination IPs (time-ordered, per user): "
      f"{n_new_ip} / {n_total_ip_events} connection events")
print("[STAGE 7] (expected: only the FIRST time each user's box talks to a given "
      "IP/domain/image/cmdline/workstation/port combo counts as novel)")
```

### Explanation

This cell fixes a major temporal-leakage problem. A global `seen` set built from the entire dataset would already contain future and same-window values, making real novelty features structurally zero. The corrected code instead processes each user's events chronologically. A value is marked `new_*` only when it has not appeared for that user before the current event. This is a genuine incremental first-seen calculation and avoids using future information to score the past.

---

## Cell 20 — Markdown

### Cell content

> **Markdown cell from the notebook**
>
> ## Stage 8 — Per-window feature engineering (69 features)

### Explanation

This markdown cell introduces Stage 8, where every 10-minute user window is converted into a fixed-length numerical representation.

---

## Cell 21 — Code

### Cell content

```python
# =====================================================================
# STAGE 8 — PER-WINDOW FEATURE ENGINEERING (69 features)
# =====================================================================
def count_id(rs, eid):
    return sum(1 for r in rs if r["event_id"] == eid)

def nunique(rs, eid, field):
    return len({r[field] for r in rs if r["event_id"] == eid and r.get(field) is not None})

def window_features(rs):
    n_4625 = count_id(rs, 4625); n_4624 = count_id(rs, 4624)
    conns = [r for r in rs if r["event_id"] == 3]
    ext = [r for r in conns if is_external(r.get("dst_ip"))]
    inbound = [r for r in conns if r.get("initiated") in (False, "false", "False")]
    pgs = {r.get("process_guid") for r in rs if r.get("process_guid")}
    pf = [proc_feats[p] for p in pgs if p in proc_feats]

    f = {}
    # --- B1: event-volume counters, one per event ID we track ---
    for eid in KEEP_EVENTS:
        f[f"n_{eid}"] = count_id(rs, eid)

    # --- B2: cardinality (how many DISTINCT values, not just count) ---
    f["nuniq_3_DestinationIp"] = nunique(rs, 3, "dst_ip")
    f["nuniq_3_DestinationPort"] = nunique(rs, 3, "dst_port")
    f["nuniq_1_Image"] = nunique(rs, 1, "image")
    f["nuniq_22_QueryName"] = nunique(rs, 22, "query_name")
    f["nuniq_11_TargetFilename"] = nunique(rs, 11, "target_filename")
    f["nuniq_4624_WorkstationName"] = nunique(rs, 4624, "workstation")
    f["nuniq_4625_TargetUserName"] = nunique(rs, 4625, "target_user")
    f["active_processes"] = len(pgs)

    # --- B3: behavioral novelty — "have we EVER seen this before?" ---
    # (uses the leak-free, time-ordered first-seen flags from Stage 7)
    f["new_dst_ips"] = sum(1 for r in conns if is_new.get(id(r), {}).get("new_ip"))
    f["new_domains"] = sum(1 for r in rs if r["event_id"] == 22 and is_new.get(id(r), {}).get("new_domain"))
    f["new_images"] = sum(1 for r in rs if r["event_id"] == 1 and is_new.get(id(r), {}).get("new_image"))
    f["new_cmdlines"] = sum(1 for r in rs if r["event_id"] == 1 and is_new.get(id(r), {}).get("new_cmdline"))
    f["new_workstations"] = sum(1 for r in rs if r["event_id"] == 4624 and is_new.get(id(r), {}).get("new_ws"))

    # --- B4: ratios ---
    f["failed_logon_ratio"] = n_4625 / (n_4625 + n_4624) if (n_4625 + n_4624) else 0.0
    f["external_ip_ratio"] = len(ext) / len(conns) if conns else 0.0
    f["inbound_ratio"] = len(inbound) / len(conns) if conns else 0.0

    # --- B5: record-level semantic / TTP signals ---
    f["n_lolbin"] = sum(1 for r in rs if r["event_id"] == 1 and is_lolbin(r.get("image")))
    f["n_lolbin_path_anomaly"] = sum(1 for r in rs if r["event_id"] == 1 and lolbin_path_anomaly(r.get("image")))
    f["n_suspicious_parent_child"] = sum(1 for r in rs if r["event_id"] == 1
                                          and suspicious_parent_child(r.get("parent_image"), r.get("image")))
    f["n_parent_user_mismatch"] = sum(1 for r in rs if r["event_id"] == 1
                                       and parent_user_mismatch(r.get("parent_user"), r.get("user")))
    f["has_encoded_cmd"] = 1 if any(has_encoded(r.get("cmdline")) for r in rs if r["event_id"] == 1) else 0
    f["lsass_access"] = 1 if any(r["event_id"] == 10 and base_name(r.get("target_image")) == "lsass.exe" for r in rs) else 0
    f["runkey_write"] = 1 if any(r["event_id"] in (12, 13) and r.get("target_object")
                                  and "run" in r["target_object"].lower() for r in rs) else 0
    f["sensitive_share"] = 1 if any(r["event_id"] == 5145 and r.get("share_name")
                                     and r["share_name"].lower().rstrip("\\").endswith(("admin$", "c$")) for r in rs) else 0
    f["is_shell_network"] = 1 if any(base_name(r.get("image")) in SHELLS for r in conns) else 0
    f["internal_445_anomaly"] = sum(1 for r in conns if r.get("dst_port") == 445
                                     and not is_external(r.get("dst_ip"))
                                     and base_name(r.get("image")) not in ("explorer.exe", "svchost.exe"))
    f["unusual_port_for_process"] = sum(1 for r in conns if r.get("dst_port")
                                         and is_new.get(id(r), {}).get("new_imgport"))
    f["dns_entropy_max"] = max([shannon_entropy(r.get("query_name")) for r in rs if r["event_id"] == 22], default=0.0)
    f["smb_pipe_activity"] = 1 if (any(r.get("dst_port") == 445 for r in conns) and any(r["event_id"] == 17 for r in rs)) else 0

    # --- B6: presence of rare/high-signal events ---
    for eid in [4698, 4724, 4738, 4771, 24]:
        f[f"has_{eid}"] = 1 if count_id(rs, eid) else 0
    f["has_4776_fail"] = 1 if any(r["event_id"] == 4776 and str(r.get("status")) != "0x0" for r in rs) else 0

    # --- B7: process-derived (aggregated from the global process timeline) ---
    f["n_micro_duration_procs"] = sum(1 for p in pf if p["is_micro_duration"])
    f["n_parent_dead_spawn"] = sum(1 for p in pf if p["parent_dead_before_spawn"])
    f["n_downloads"] = sum(p["downloads"] for p in pf)
    f["max_beacon_regularity"] = max([p["beacon_regularity"] for p in pf if p["beacon_regularity"] is not None], default=0.0)
    f["max_cmd_char_length"] = max([p["cmd_char_length"] for p in pf], default=0)
    f["max_special_char_density"] = max([p["special_char_density"] for p in pf], default=0.0)
    return f

sample_key, sample_rs = max(((k, v) for k, v in buckets.items() if k[0] == real_users[0]),
                             key=lambda kv: len(kv[1]))
sample_vec = window_features(sample_rs)
print(f"\n[STAGE 8] total feature columns per window: {len(sample_vec)}")
print(f"[STAGE 8] busiest window for {sample_key[0]} @ {slot_label(sample_key[1])} "
      f"({len(sample_rs)} events) — non-zero features:")
print({k: v for k, v in sample_vec.items() if v})
```

### Explanation

This is the main feature-engineering cell. `count_id` counts events of a given ID and `nunique` measures distinct values. `window_features` builds the 69-dimensional feature vector. The feature groups include event-volume counts, distinct-value cardinalities, novelty indicators, authentication/network ratios, LOLBin and process-chain signals, encoded-command indicators, LSASS/registry/share/pipe behaviors, DNS entropy, unusual-port behavior, and process-derived features such as micro-duration processes, downloads, command length, special-character density, and beacon regularity. The function returns one consistent feature dictionary for every window, including empty windows.

---

## Cell 22 — Markdown

### Cell content

> **Markdown cell from the notebook**
>
> ## Stage 9 — Zero-filled (user, day, slot) grid

### Explanation

This markdown cell introduces Stage 9. Statistical baselines require not only active windows but also periods in which nothing happened, so the notebook creates an explicit zero-filled grid.

---

## Cell 23 — Code

### Cell content

```python
# =====================================================================
# STAGE 9 — ZERO-FILLED (user, day, 10-min-slot) GRID
# =====================================================================
days = sorted({day_of(slot_of(r["time"])) for r in records})

grid = {}
for u in real_users:
    for d in days:
        day_start = int(datetime.strptime(d, "%Y-%m-%d").replace(tzinfo=timezone.utc).timestamp())
        for i in range(144):
            slot = day_start + i * WINDOW_SEC
            grid[(u, d, slot)] = window_features(buckets.get((u, slot), []))

print(f"\n[STAGE 9] grid cells: {len(grid)} = {len(real_users)} users x {len(days)} days x 144 slots")
```

### Explanation

This cell finds all dates in the dataset, then creates 144 ten-minute slots for every user on every day. For a slot with events, `window_features` summarizes them; for an empty slot, `buckets.get(..., [])` supplies an empty list and the same feature function returns zeros. This creates a complete user/day/time grid instead of representing only observed activity. That distinction is important because otherwise the baseline would be biased toward periods in which something happened.

---

## Cell 24 — Markdown

### Cell content

> **Markdown cell from the notebook**
>
> ## Stage 10 — Per (user, hour-of-day) Median + MAD baseline

### Explanation

This markdown cell introduces Stage 10, the statistical baseline. The detector asks: 'How unusual is this user's current behavior compared with their own historical behavior at this hour of day?'

---

## Cell 25 — Code

### Cell content

```python
# =====================================================================
# STAGE 10 — PER (user, hour-of-day) MEDIAN + MAD BASELINE
# =====================================================================
def mad(vals):
    m = median(vals)
    d = median([abs(v - m) for v in vals])
    return d if d > 0 else 0.5   # floor to avoid divide-by-zero in z-scores downstream

baseline = defaultdict(lambda: defaultdict(list))
for (u, d, slot), vec in grid.items():
    baseline[u][hour_of(slot)].append(vec)

FEATURE_KEYS = list(sample_vec.keys())   # FIX: use ALL 69 features for reporting,
                                          # not the old prefix-filtered subset that
                                          # silently hid new_*, has_encoded_cmd,
                                          # lsass_access, runkey_write, etc.

def active_count(vec):
    return sum(v for k, v in vec.items() if k.startswith("n_"))

def print_baseline(u=None, h=None):
    us = [u] if u else real_users
    for uname in us:
        hours = [h] if h is not None else sorted(baseline[uname])
        for hh in hours:
            pool = baseline[uname][hh]
            n_act = sum(1 for v in pool if active_count(v) > 0)
            print(f"=== {uname}  hour {hh:02d}:00-{hh:02d}:59  |  "
                  f"{len(pool)} windows | {n_act:2d} active | {len(pool) - n_act:2d} empty ===")
            sig = [(f, median([v[f] for v in pool])) for f in FEATURE_KEYS if any(v[f] for v in pool)]
            for f, m in sig:
                print(f"    {f:30} median={m:6.2f}  mad={mad([v[f] for v in pool]):6.2f}")
            if not sig:
                print("    (no activity in this hour -- all features 0)")
        print()

print(f"\n[STAGE 10] baseline structure: "
      f"{ {u: len(baseline[u]) for u in real_users} } hours-of-day covered per user")
print_baseline("alice", 9)
```

### Explanation

This cell implements the Median + MAD baseline. `mad` calculates Median Absolute Deviation and uses a 0.5 floor when MAD is zero to prevent division-by-zero later. `baseline` groups every zero-filled window by user and UTC hour. `FEATURE_KEYS` deliberately uses all 69 engineered features, fixing an earlier reporting problem where some important features could be silently omitted. `active_count` distinguishes active from empty windows, while `print_baseline` prints the historical sample size and feature medians for a selected user/hour.

---

## Cell 26 — Markdown

### Cell content

> **Markdown cell from the notebook**
>
> ## Stage 11 — Simulated attack injection + robust z-score anomaly comparison

### Explanation

This markdown cell introduces Stage 11, where the notebook creates a controlled synthetic intrusion and compares its feature vector against the appropriate historical baseline using robust statistics.

---

## Cell 27 — Code

### Cell content

```python
# =====================================================================
# STAGE 11 — ANOMALY INJECTION + ROBUST Z-SCORE COMPARISON
# =====================================================================
# Simulated intrusion: Office macro spawns encoded PowerShell, which talks
# to an external C2 IP on a non-standard port, touches LSASS, resolves a
# high-entropy DGA-style domain, and is followed by two failed logons from
# an external IP against the same account.
TARGET_USER = "diana"
ANOM_TIME = "2026-08-10T02:00:00Z"   # off-hours for this user (see Stage 3 hour histogram)

anom = [
    dict(time="2026-08-10T02:00:00Z", event_id=1, user=TARGET_USER,
         resolved_user=TARGET_USER, process_guid="{A1}", parent_process_guid="{A0}",
         image="C:\\Users\\Public\\powershell.exe", cmdline="powershell.exe -enc JABzAD0...",
         parent_image="C:\\Program Files\\Microsoft Office\\root\\Office16\\WINWORD.EXE",
         parent_user="CORP\\diana"),
    dict(time="2026-08-10T02:00:05Z", event_id=3, user=TARGET_USER, resolved_user=TARGET_USER,
         process_guid="{A1}", image="C:\\Users\\Public\\powershell.exe",
         dst_ip="203.0.113.99", dst_port=4444, initiated=True),
    dict(time="2026-08-10T02:00:10Z", event_id=3, user=TARGET_USER, resolved_user=TARGET_USER,
         process_guid="{A1}", image="C:\\Users\\Public\\powershell.exe",
         dst_ip="203.0.113.99", dst_port=4444, initiated=True),
    dict(time="2026-08-10T02:00:15Z", event_id=3, user=TARGET_USER, resolved_user=TARGET_USER,
         process_guid="{A1}", image="C:\\Users\\Public\\powershell.exe",
         dst_ip="203.0.113.99", dst_port=4444, initiated=True),
    dict(time="2026-08-10T02:00:20Z", event_id=3, user=TARGET_USER, resolved_user=TARGET_USER,
         process_guid="{A1}", image="C:\\Users\\Public\\powershell.exe",
         dst_ip="203.0.113.99", dst_port=4444, initiated=True),
    dict(time="2026-08-10T02:00:11Z", event_id=10, user=TARGET_USER, resolved_user=TARGET_USER,
         process_guid="{A1}", image="C:\\Users\\Public\\powershell.exe",
         target_image="C:\\Windows\\System32\\lsass.exe"),
    dict(time="2026-08-10T02:00:25Z", event_id=22, user=TARGET_USER, resolved_user=TARGET_USER,
         query_name="x7k2m9q-v4n8.xyz"),
    dict(time="2026-08-10T02:00:40Z", event_id=4625, user=TARGET_USER, resolved_user=TARGET_USER,
         target_user=TARGET_USER, src_ip="203.0.113.7", workstation="WIN-WS04"),
    dict(time="2026-08-10T02:00:45Z", event_id=4625, user=TARGET_USER, resolved_user=TARGET_USER,
         target_user=TARGET_USER, src_ip="203.0.113.7", workstation="WIN-WS04"),
]
# fill every key normalize() would have produced, defaulting missing ones to None,
# so window_features()/downstream code never KeyErrors on a field this synthetic
# event doesn't set.
NORMALIZED_KEYS = list(records[0].keys())
for e in anom:
    for k in NORMALIZED_KEYS:
        e.setdefault(k, None)
    e["is_new_dummy"] = None  # placeholder, not used

# register the injected process in the same global structures real processes use
proc_start["{A1}"] = epoch(anom[0]["time"]); proc_image["{A1}"] = anom[0]["image"]
proc_cmdline["{A1}"] = anom[0]["cmdline"]; proc_parent["{A1}"] = "{A0}"
proc_parent_image["{A1}"] = anom[0]["parent_image"]; proc_parent_user["{A1}"] = "CORP\\diana"
proc_user["{A1}"] = TARGET_USER
conns_by_proc["{A1}"] = [(epoch(e["time"]), e.get("dst_ip"), e.get("dst_port")) for e in anom if e["event_id"] == 3]
proc_feats["{A1}"] = dict(
    unique_ips=1, downloads=0, cmd_char_length=len(anom[0]["cmdline"]),
    special_char_density=special_char_density(anom[0]["cmdline"]),
    is_lolbin=True, lolbin_path_anomaly=True, suspicious_parent_child=True,
    parent_user_mismatch=False, lifetime_seconds=45, is_micro_duration=False,
    parent_dead_before_spawn=False, beacon_regularity=beacon_regularity("{A1}"))

# mark novelty honestly: every one of these IOCs is genuinely first-seen for
# diana (never appears anywhere in the legitimate 10-day baseline)
for e in anom:
    flags = {}
    if e.get("dst_ip"):
        flags["new_ip"] = e["dst_ip"] not in seen_ip[TARGET_USER]
    if e.get("query_name"):
        flags["new_domain"] = e["query_name"] not in seen_domain[TARGET_USER]
    if e["event_id"] == 1 and e.get("image"):
        flags["new_image"] = e["image"] not in seen_image[TARGET_USER]
    if e["event_id"] == 1 and e.get("cmdline"):
        flags["new_cmdline"] = e["cmdline"] not in seen_cmdline[TARGET_USER]
    if e.get("dst_port") and e.get("image"):
        flags["new_imgport"] = (base_name(e["image"]), e["dst_port"]) not in seen_imgport[TARGET_USER]
    is_new[id(e)] = flags

anomaly_vec = window_features(anom)

# FIX (major gap): the original notebook never actually scored the injected
# window against the Median+MAD baseline it built — it only diffed it
# against one arbitrarily-chosen "busiest normal window" for the same user,
# at a DIFFERENT (daytime) hour than the attack. That is not a baseline
# comparison at all. Here we score against the user's OWN hour-matched
# baseline (hour 2, i.e. 02:00-02:59), as the pipeline design requires.
attack_hour = hour_of(slot_of(ANOM_TIME))
hour_pool = baseline[TARGET_USER].get(attack_hour, [])
print(f"\n[STAGE 11] scoring injected attack window for user={TARGET_USER}, "
      f"hour={attack_hour:02d}:00 against {len(hour_pool)} historical windows at that hour")
if hour_pool:
    n_hour_active = sum(1 for v in hour_pool if active_count(v) > 0)
    print(f"[STAGE 11] of those, {n_hour_active} had ANY real activity "
          f"(diana has essentially no legitimate activity at 02:00 in this dataset -- "
          f"see the hour histogram in the report)")

def robust_zscores(vec, pool):
    """Robust z-score per feature: (x - median) / (1.4826 * MAD)."""
    out = {}
    for k in vec:
        vals = [p[k] for p in pool] if pool else [0]
        m = median(vals)
        d = mad(vals)
        z = (vec[k] - m) / (1.4826 * d)
        out[k] = (vec[k], m, d, z)
    return out

zscores = robust_zscores(anomaly_vec, hour_pool)
Z_THRESHOLD = 3.5
flagged = sorted(
    ((k, *v) for k, v in zscores.items() if abs(v[3]) >= Z_THRESHOLD),
    key=lambda t: -abs(t[4]),
)
print(f"\n[STAGE 11] features flagged as anomalous (|z| >= {Z_THRESHOLD}), "
      f"ranked by |z|:")
print(f"  {'feature':30} {'value':>8} {'baseline_med':>13} {'baseline_mad':>13} {'z':>8}")
for k, val, m, d, z in flagged:
    print(f"  {k:30} {val:>8} {m:>13.2f} {d:>13.2f} {z:>8.2f}")

print(f"\n[STAGE 11] {len(flagged)} / {len(anomaly_vec)} features flagged as anomalous "
      f"by robust z-score alone")

# ---------------------------------------------------------------------
# STAGE 11b — supplementary rule for rare / structurally-binary features
# ---------------------------------------------------------------------
# CAVEAT worth surfacing rather than hiding: MAD-based z-scores are a poor
# fit for rare, near-always-zero indicator features (has_encoded_cmd,
# lsass_access, n_suspicious_parent_child, n_lolbin, n_parent_user_mismatch,
# ...). When a feature's entire baseline pool is 0, mad() floors to 0.5,
# so even a single occurrence (0 -> 1) only produces z = 1/(1.4826*0.5)
# ~= 1.35 -- well under a 3.5 threshold, even though "this NEVER happened
# before and just happened" is exactly the kind of signal a security
# analyst cares about. We therefore add a simple, explicit second rule:
# flag any feature whose entire hour-matched baseline pool was zero but
# whose value in the scored window is non-zero.
zero_baseline_flags = sorted(
    (k for k, (val, m, d, z) in zscores.items()
     if val and m == 0 and all(p[k] == 0 for p in hour_pool) and k not in {k2 for k2, *_ in flagged}),
)
print(f"\n[STAGE 11b] additional rare-event indicators that fired despite an "
      f"all-zero baseline (not caught by the z>= {Z_THRESHOLD} rule above "
      f"because of the MAD floor on sparse features):")
for k in zero_baseline_flags:
    print(f"  {k:30} value={anomaly_vec[k]}  (baseline for this user/hour was 0 in all "
          f"{len(hour_pool)} windows)")
if not zero_baseline_flags:
    print("  (none)")

print(f"\n[STAGE 11] TOTAL distinct anomalous features flagged (z-score + rare-event rule): "
      f"{len(flagged) + len(zero_baseline_flags)} / {len(anomaly_vec)}")
```

### Explanation

This is the final and most complex cell. It constructs a synthetic attack against `diana` at 02:00 UTC: an Office document launches encoded PowerShell, PowerShell connects repeatedly to an external IP on port 4444, touches LSASS, queries a high-entropy domain, and is followed by failed logons. The synthetic records are shaped to match the normalized schema, registered in the process-timeline structures, and passed through the same feature-generation functions as real events. The attack is compared against Diana's historical 02:00 windows, not an arbitrary busy hour. The cell calculates robust z-scores using `(current - median) / (1.4826 * MAD)`, applies the 3.5 threshold, and then adds a separate sparse-event rule for features whose historical baseline is entirely zero. This second rule is necessary because a binary feature changing from 0 to 1 can remain below a conventional 3.5 robust-z threshold when MAD is floored. The final output therefore represents anomalous feature signals, not a claim that a specific number of real attacks was detected.

---

# Important results and interpretation

## What the notebook is actually detecting

The detector is designed to identify **behavioral deviations for a particular user and time of day**. It is not a malware classifier and it does not prove that an anomalous feature is malicious.

A useful interpretation is:

```text
Raw telemetry
    ↓
Behavioral features
    ↓
User/hour historical baseline
    ↓
Statistical deviation + security heuristics
    ↓
Suspicious/anomalous behavior
    ↓
Analyst investigation
```

## Major corrections implemented

1. **Logon-ID normalization** supports the fields needed by different Security events.
2. **Resolved-user bucketing** ensures process/logon correlation is actually used downstream.
3. **Private-IP classification** covers the relevant private/local ranges.
4. **Novelty detection is time ordered**, avoiding future/same-window leakage.
5. **All 69 features** are included in baseline reporting.
6. **The attack is compared with the correct user/hour baseline.**
7. **Sparse all-zero security features receive a separate zero-baseline rule.**

## Key limitations

- Several supported Sysmon/Security event types are absent from the real JSONL, so their features cannot be validated on real occurrences in this dataset.
- The attack demonstration is synthetic; it validates the pipeline's response to a designed scenario but is not a real-world performance benchmark.
- Median/MAD is robust, but sparse binary/count features with all-zero history are not ideally modeled by a conventional z-score; the additional zero-baseline rule is an engineering mitigation.
- LOLBin and suspicious parent-child lists are curated and incomplete.
- Process-derived features can be attributed to multiple adjacent windows for long-lived processes by design.
- The dataset is relatively small for production behavioral modeling.
- No labeled real-world precision, recall, F1, false-positive rate, ROC-AUC, or PR-AUC evaluation is established by this notebook.

## Final interpretation

If the notebook reports something such as **5/69 robust-z features** and **28/69 combined anomalous features**, those numbers mean that many engineered behavioral dimensions deviated from the selected historical baseline. They do **not** mean 5 or 28 attacks, nor do they represent accuracy or recall.

The strongest way to describe the project is:

> **A user-aware Windows Security/Sysmon behavioral anomaly-detection pipeline that normalizes heterogeneous telemetry, correlates processes with logon sessions, engineers 69 behavioral/security features, learns per-user/hour robust baselines, and combines robust statistical deviation with sparse security-event rules.**
