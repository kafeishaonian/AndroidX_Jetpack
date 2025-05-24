//
// Created by 64860 on 2025/5/6.
//

#ifndef ANDROIDX_JETPACK_EVENT_LOOP_H
#define ANDROIDX_JETPACK_EVENT_LOOP_H

#include <functional>
#include <sys/epoll.h>
#include <unordered_map>

using namespace std;

namespace p2p {


    class EventLoop {

    public:
        using EventCallback = function<void(int, uint32_t)>;

        EventLoop();
        ~EventLoop();

        bool init();
        void run();
        void stop();

        bool addEvent(int fd, uint32_t events, const EventCallback& callback);
        bool modEvent(int fd, uint32_t events);
        bool delEvent(int fd);


    private:
        int _epoll_fd;
        bool _running;
        unordered_map<int, EventCallback> _callbacks;
    };

}

#endif //ANDROIDX_JETPACK_EVENT_LOOP_H
