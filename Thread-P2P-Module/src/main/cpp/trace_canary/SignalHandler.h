//
// Created by 魏红明 on 2025/7/31.
//

#ifndef ANDROIDX_JETPACK_SIGNALHANDLER_H
#define ANDROIDX_JETPACK_SIGNALHANDLER_H

#include <signal.h>
#include <malloc.h>
#include <syscall.h>
#include <unistd.h>
#include <dirent.h>

#include <mutex>
#include <vector>
#include <algorithm>
#include <cinttypes>

using namespace std;

namespace anr_trace {

    class SignalHandler {

    public:
        SignalHandler();
        virtual ~SignalHandler();

    protected:
        enum Result { NOT_HANDLED = 0, HANDLED, HANDLED_NO_RETRIGGER };
        virtual void handleSignal(int sig, const siginfo_t *info, void *uc) = 0;
        virtual void handleDebuggerSignal(int sig, const siginfo_t *info, void *uc) = 0;
        static bool installHandlersLocked();
        static bool installNativeBacktraceHandlersLocked();
        static void restoreHandlersLocked();
        static void restoreNativeBacktraceHandlersLocked();
        static void installDefaultHandler(int sig);

        static const int TARGET_SIG = SIGQUIT;
        static const int BIONIC_SIGNAL_DEBUGGER = (__SIGRTMIN + 3);


    private:
        static void signalHeader(int sig, siginfo_t *info, void *uc);
        static void debuggerSignalHandler(int sig, siginfo_t *info, void *uc);
        static void installAlternateStackLocked();

        SignalHandler(const SignalHandler &) = delete;
        SignalHandler &operator=(const SignalHandler &) = delete;


        static bool sHandlerInstalled;
        static struct sigaction sOldHandlers;
        static mutex sHandlerStackMutex;
        static vector<SignalHandler*>* sHandlerStack;
        static bool sStackInstalled;
        static stack_t sOldStack;
        static stack_t sNewStack;
        static bool sNativeBacktraceHandlerInstalled;
        static struct sigaction sNativeBacktraceOldHandlers;
        static mutex sNativeBacktraceHandlerStackMutex;
    };
}

#endif //ANDROIDX_JETPACK_SIGNALHANDLER_H
