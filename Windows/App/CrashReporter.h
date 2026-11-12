// Local crash diagnostics. Reports are written to data/crashes and never uploaded.
#pragma once
#include <windows.h>
namespace vikey { namespace crash {
void install();
void selfTestIfAsked();
}}
