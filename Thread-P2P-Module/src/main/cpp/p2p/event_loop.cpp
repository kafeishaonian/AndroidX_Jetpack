//
// Created by 64860 on 2025/5/6.
//

#include "event_loop.h"
#include <sys/epoll.h>
#include <unistd.h>
#include <stdexcept>
#include "../utils/log_utils.h"

#define TAG "event_loop.h"

namespace p2p {

    EventLoop::EventLoop() :_epoll_fd(-1), _running(false) {}
    
    
    EventLoop::~EventLoop() {
        stop();
        if (_epoll_fd >= 0) {
            close(_epoll_fd);
        }
    }

    bool EventLoop::init() {
        _epoll_fd = epoll_create1(0);
        if (_epoll_fd < 0) {
            return false;
        }
        return true;
    }


    void EventLoop::run() {
        const int MAX_EVENTS = 64;
        struct epoll_event events[MAX_EVENTS];
        
        _running = true;

        while (_running) {
            int num_events = epoll_wait(_epoll_fd, events, MAX_EVENTS, -1);
            if (num_events < 0) {
                if (errno == EINTR) {
                    continue;
                }
                break;
            }

            for (int i = 0; i < num_events; ++i) {
                int fd = events[i].data.fd;
                uint32_t revents = events[i].events;

                auto it = _callbacks.find(fd);
                if (it != _callbacks.end()) {
                    it->second(fd, revents);
                }
            }
        }
    }


    void EventLoop::stop() {
        _running = false;
    }

    bool
    EventLoop::addEvent(int fd, uint32_t events, const p2p::EventLoop::EventCallback &callback) {
        struct epoll_event ev;
        ev.events = events;
        ev.data.fd = fd;

        if (epoll_ctl(_epoll_fd, EPOLL_CTL_ADD, fd, &ev) < 0) {
            return false;
        }

        _callbacks[fd] = callback;
        return true;
    }

    bool EventLoop::modEvent(int fd, uint32_t events) {
        struct epoll_event ev;
        ev.events = events;
        ev.data.fd = fd;

        return epoll_ctl(_epoll_fd, EPOLL_CTL_MOD, fd, &ev) == 0;
    }


    bool EventLoop::delEvent(int fd) {
        if (epoll_ctl(_epoll_fd, EPOLL_CTL_DEL, fd, nullptr) < 0) {
            return false;
        }

        _callbacks.erase(fd);
        return false;
    }

}