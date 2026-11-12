#include "Prefs.h"
#include <windows.h>
#include <vector>
#include <string>
#include <algorithm>
#include <cwchar>
#include <cstdlib>

namespace vikey { namespace prefs {

static const wchar_t* RUN_KEY = L"Software\\Microsoft\\Windows\\CurrentVersion\\Run";
static const wchar_t* SECTION = L"Settings";

static std::wstring dataDirectory() {
    wchar_t exe[MAX_PATH] = {0};
    DWORD n = GetModuleFileNameW(nullptr, exe, MAX_PATH);
    if (!n || n >= MAX_PATH) return L"data";
    wchar_t* slash = wcsrchr(exe, L'\\');
    wchar_t* forward = wcsrchr(exe, L'/');
    if (!slash || (forward && forward > slash)) slash = forward;
    if (slash) *slash = L'\0';
    return std::wstring(exe) + L"\\data";
}

static std::wstring settingsPath() {
    return dataDirectory() + L"\\settings.ini";
}

static void ensureDataDirectory() {
    const std::wstring dir = dataDirectory();
    CreateDirectoryW(dir.c_str(), nullptr);
}

static std::wstring readRaw(const wchar_t* section, const wchar_t* key) {
    wchar_t value[32768] = {0};
    GetPrivateProfileStringW(section, key, L"", value, (DWORD)(sizeof(value) / sizeof(value[0])), settingsPath().c_str());
    return value;
}

static void writeRaw(const wchar_t* section, const wchar_t* key, const std::wstring& value) {
    ensureDataDirectory();
    WritePrivateProfileStringW(section, key, value.c_str(), settingsPath().c_str());
}

static std::wstring escape(const std::wstring& value) {
    std::wstring out;
    for (wchar_t c : value) {
        if (c == L'%') out += L"%25";
        else if (c == L'\r') out += L"%0D";
        else if (c == L'\n') out += L"%0A";
        else if (c == L'|') out += L"%7C";
        else out += c;
    }
    return out;
}

static std::wstring unescape(const std::wstring& value) {
    std::wstring out;
    for (size_t i = 0; i < value.size(); ++i) {
        if (value[i] == L'%' && i + 2 < value.size()) {
            const std::wstring code = value.substr(i, 3);
            if (code == L"%25") { out += L'%'; i += 2; continue; }
            if (code == L"%0D") { out += L'\r'; i += 2; continue; }
            if (code == L"%0A") { out += L'\n'; i += 2; continue; }
            if (code == L"%7C") { out += L'|'; i += 2; continue; }
        }
        out += value[i];
    }
    return out;
}

static void writeIntRaw(const wchar_t* key, int value) {
    wchar_t number[32]; _snwprintf_s(number, _TRUNCATE, L"i:%d", value);
    writeRaw(SECTION, key, number);
}

static void writeStringRaw(const wchar_t* key, const std::wstring& value) {
    writeRaw(SECTION, key, L"s:" + escape(value));
}

static void writeBinaryRaw(const wchar_t* key, const BYTE* data, DWORD size) {
    static const wchar_t hex[] = L"0123456789ABCDEF";
    std::wstring value = L"b:";
    value.reserve(2 + size * 2);
    for (DWORD i = 0; i < size; ++i) {
        value += hex[data[i] >> 4];
        value += hex[data[i] & 0x0F];
    }
    writeRaw(SECTION, key, value);
}

static void writeStringListRaw(const wchar_t* key, const std::vector<std::wstring>& values) {
    std::wstring value = L"l:";
    for (size_t i = 0; i < values.size(); ++i) {
        if (i) value += L'|';
        value += escape(values[i]);
    }
    writeRaw(SECTION, key, value);
}

bool has(const wchar_t* key) {
    return !readRaw(SECTION, key).empty();
}

int getInt(const wchar_t* key, int fallback) {
    const std::wstring raw = readRaw(SECTION, key);
    if (raw.empty()) return fallback;
    const wchar_t* p = raw.c_str();
    if (raw.size() >= 2 && raw[1] == L':') {
        if (raw[0] != L'i') return fallback;
        p += 2;
    }
    wchar_t* end = nullptr;
    long value = wcstol(p, &end, 10);
    return end != p ? (int)value : fallback;
}

void setInt(const wchar_t* key, int value) { writeIntRaw(key, value); }

std::vector<BYTE> getBinary(const wchar_t* key) {
    std::vector<BYTE> out;
    const std::wstring raw = readRaw(SECTION, key);
    if (raw.size() < 2 || raw[0] != L'b' || raw[1] != L':') return out;
    const size_t count = (raw.size() - 2) / 2;
    if ((raw.size() - 2) % 2) return out;
    out.reserve(count);
    for (size_t i = 0; i < count; ++i) {
        wchar_t pair[3] = {raw[2 + i * 2], raw[3 + i * 2], 0};
        wchar_t* end = nullptr;
        unsigned long value = wcstoul(pair, &end, 16);
        if (end != pair + 2) { out.clear(); return out; }
        out.push_back((BYTE)value);
    }
    return out;
}

void setBinary(const wchar_t* key, const BYTE* data, DWORD size) {
    if (!data && size) return;
    writeBinaryRaw(key, data, size);
}

std::wstring getString(const wchar_t* key, const std::wstring& fallback) {
    const std::wstring raw = readRaw(SECTION, key);
    if (raw.size() < 2 || raw[0] != L's' || raw[1] != L':') return fallback;
    return unescape(raw.substr(2));
}

void setString(const wchar_t* key, const std::wstring& value) { writeStringRaw(key, value); }

std::vector<std::wstring> getStringList(const wchar_t* key) {
    std::vector<std::wstring> out;
    const std::wstring raw = readRaw(SECTION, key);
    if (raw.size() < 2 || raw[0] != L'l' || raw[1] != L':') return out;
    size_t start = 2;
    while (start <= raw.size()) {
        size_t end = raw.find(L'|', start);
        if (end == std::wstring::npos) end = raw.size();
        if (end > start) out.push_back(unescape(raw.substr(start, end - start)));
        if (end == raw.size()) break;
        start = end + 1;
    }
    return out;
}

void setStringList(const wchar_t* key, const std::vector<std::wstring>& values) {
    writeStringListRaw(key, values);
}

void remove(const wchar_t* key) {
    WritePrivateProfileStringW(SECTION, key, nullptr, settingsPath().c_str());
}

std::wstring appDataDir() {
    ensureDataDirectory();
    return dataDirectory();
}

std::wstring logsDir() {
    const std::wstring dir = appDataDir() + L"\\logs";
    CreateDirectoryW(dir.c_str(), nullptr);
    return dir;
}

bool launchAtLogin() {
    HKEY h = nullptr;
    if (RegOpenKeyExW(HKEY_CURRENT_USER, RUN_KEY, 0, KEY_READ, &h) != ERROR_SUCCESS) return false;
    DWORD size = 0;
    const bool enabled = RegQueryValueExW(h, L"Vikey", nullptr, nullptr, nullptr, &size) == ERROR_SUCCESS;
    RegCloseKey(h);
    return enabled;
}

void setLaunchAtLogin(bool on) {
    // Windows startup integration remains in the standard per-user Run registry key.
    HKEY h = nullptr;
    if (RegCreateKeyExW(HKEY_CURRENT_USER, RUN_KEY, 0, nullptr, 0, KEY_WRITE, nullptr, &h, nullptr) != ERROR_SUCCESS) return;
    if (on) {
        wchar_t exe[MAX_PATH] = {0};
        DWORD n = GetModuleFileNameW(nullptr, exe, MAX_PATH);
        if (n > 0 && n < MAX_PATH) {
            std::wstring cmd = L"\"" + std::wstring(exe) + L"\"";
            RegSetValueExW(h, L"Vikey", 0, REG_SZ, (const BYTE*)cmd.c_str(), (DWORD)((cmd.size() + 1) * sizeof(wchar_t)));
        }
    } else {
        RegDeleteValueW(h, L"Vikey");
    }
    RegCloseKey(h);
}

}} // namespace
