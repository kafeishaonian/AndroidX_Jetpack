//
// Created by 魏红明 on 2025/7/31.
//

#include "SignalHandler.h"
#include "../utils/log_utils.h"

#define LOG_TAG "SignalHandler"


namespace anr_trace{

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

        for(auto it = sHandlerStack->rbegin(); it != sHandlerStack->rend(); ++it) {
            (*it)->handlerDebuggerSignal(sig, info, uc);
        }
        lock.unlock();
    }
}