//
// Created by 魏红明 on 2025/7/31.
//

#include "SignalHandler.h"
#include "../utils/log_utils.h"

#define LOG_TAG "SignalHandler"


namespace anr_trace{

    bool SignalHandler::sHandlerInstalled = false;
    vector<SignalHandler*>* SignalHandler::sHandlerStack = nullptr;
    bool SignalHandler::sStackInstalled = false;
    bool SignalHandler::sNativeBacktraceHandlerInstalled = false;

    SignalHandler::SignalHandler() {
        lock_guard<mutex> lock(sHandlerStackMutex);

        if (!sHandlerStack) {
            sHandlerStack = new vector<SignalHandler*>;
        }
        installAlternateStackLocked();
        installHandlersLocked();
        installNativeBacktraceHandlersLocked();
        sHandlerStack->push_back(this);
    }

    SignalHandler::~SignalHandler() {

    }

    void SignalHandler::installAlternateStackLocked() {
        if (sStackInstalled) {
            return;
        }

        memset(&sOldStack, 0, sizeof(sOldStack));
        memset(&sNewStack, 0, sizeof(sNewStack));
        static constexpr unsigned kSigStackSize = max(16384, SIGSTKSZ);

        if (sigaltstack(nullptr, &sOldStack) == -1 || !sOldStack.ss_sp || sOldStack.ss_size < kSigStackSize) {
            sNewStack.ss_sp = calloc(1, kSigStackSize);
            sNewStack.ss_size = kSigStackSize;
            if (sigaltstack(&sNewStack, nullptr) == -1) {
                free(sNewStack.ss_sp);
            }
        }
        sStackInstalled = true;
    }

    void SignalHandler::handleSignal(int sig, const siginfo_t *info, void *uc) {

    }

    void SignalHandler::handleDebuggerSignal(int sig, const siginfo_t *info, void *uc) {

    }

    void SignalHandler::restoreHandlersLocked() {

    }

    void SignalHandler::restoreNativeBacktraceHandlersLocked() {

    }

    void SignalHandler::debuggerSignalHandler(int sig, siginfo_t *info, void *uc) {
        unique_lock<mutex> lock(sNativeBacktraceHandlerStackMutex);

        for (auto it = sHandlerStack->rbegin(); it != sHandlerStack->rend(); ++it) {
            (*it)->handleDebuggerSignal(sig, info, uc);
        }

        lock.unlock();
    }

    bool SignalHandler::installHandlersLocked() {
        if (sHandlerInstalled) {
            return false;
        }

        if(sigaction(TARGET_SIG, nullptr, &sOldHandlers) == -1) {
            return false;
        }

        struct sigaction sa{};
        sa.sa_sigaction = signalHeader;
        sa.sa_flags = SA_ONSTACK | SA_SIGINFO | SA_RESTART;
        if (sigaction(TARGET_SIG, &sa, nullptr) == -1) {
            return false;
        }

        sHandlerInstalled = true;
        return true;
    }

    bool SignalHandler::installNativeBacktraceHandlersLocked() {
        if (sNativeBacktraceHandlerInstalled) {
            return false;
        }

        if (sigaction(BIONIC_SIGNAL_DEBUGGER, nullptr, &sNativeBacktraceOldHandlers) == -1) {
            return false;
        }

        struct sigaction sa{};
        sa.sa_sigaction = debuggerSignalHandler;
        sa.sa_flags = SA_ONSTACK | SA_SIGINFO | SA_RESTART;

        if (sigaction(BIONIC_SIGNAL_DEBUGGER, &sa, nullptr) == -1) {
            return false;
        }

        sNativeBacktraceHandlerInstalled = false;
        return true;
    }

    void SignalHandler::installDefaultHandler(int sig) {
        struct sigaction sa;
        memset(&sa, 0, sizeof(sa));
        sigemptyset(&sa.sa_mask);
        sa.sa_handler = SIG_DFL;
        sa.sa_flags = SA_RESTART;
        if (sigaction(sig, &sa, nullptr) == -1) {
            LOGE(LOG_TAG, "failed to install default handle for signal: %d", sig);
        }
    }


    void SignalHandler::signalHeader(int sig, siginfo_t *info, void *uc) {
        unique_lock<mutex> lock(sHandlerStackMutex);

        for (auto it = sHandlerStack->rbegin(); it != sHandlerStack->rend(); ++it) {
            (*it)->handleDebuggerSignal(sig, info, uc);
        }
        lock.unlock();
    }
}