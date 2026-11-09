// Preferences stored in the portable data/settings.ini file.
#pragma once
#include <windows.h>
#include <string>
#include <vector>

namespace vikey { namespace prefs {
int getInt(const wchar_t* key, int fallback);
bool has(const wchar_t* key);
void setInt(const wchar_t* key, int value);
std::vector<BYTE> getBinary(const wchar_t* key);
void setBinary(const wchar_t* key, const BYTE* data, DWORD size);
std::wstring getString(const wchar_t* key, const std::wstring& fallback);
void setString(const wchar_t* key, const std::wstring& value);
std::vector<std::wstring> getStringList(const wchar_t* key);
void setStringList(const wchar_t* key, const std::vector<std::wstring>& values);
void remove(const wchar_t* key);

// Portable data directory beside the executable. Created on demand.
std::wstring appDataDir();
std::wstring logsDir();

// Windows startup registration remains an OS integration and therefore uses the Run registry key.
bool launchAtLogin();
void setLaunchAtLogin(bool on);

}}
