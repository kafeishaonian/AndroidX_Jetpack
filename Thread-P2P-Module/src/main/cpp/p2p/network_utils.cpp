//
// Created by 64860 on 2025/5/6.
//

#include "network_utils.h"
#include <ifaddrs.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <cstring>
#include <vector>
#include "../utils/log_utils.h"

#define TAG "network_utils.h"

namespace p2p {

    string NetworkUtils::getLocalIP() {
        struct ifaddrs *ifaddr, *ifa;
        string ip;

        if (getifaddrs(&ifaddr) == -1) {
            return "";
        }

        for (ifa = ifaddr; ifa != nullptr; ifa = ifa->ifa_next) {
            if (ifa->ifa_addr == nullptr) {
                continue;
            }


            if (ifa->ifa_addr->sa_family == AF_INET) {
                struct sockaddr_in *addr = (struct sockaddr_in *) ifa->ifa_addr;
                char ipstr[INET_ADDRSTRLEN];
                inet_ntop(AF_INET, &addr->sin_addr, ipstr, INET_ADDRSTRLEN);

                //排除回环地址
                if (strcmp(ipstr, "127.0.0.1") != 0) {
                    ip = ipstr;
                    break;
                }
            }
        }
        freeifaddrs(ifaddr);
        return ip;
    }


    vector<string> NetworkUtils::getAllIPs() {
        struct ifaddrs *ifaddr, *ifa;
        vector<string> ips;

        if (getifaddrs(&ifaddr) == -1) {
            return ips;
        }

        for (ifa = ifaddr; ifa != nullptr; ifa = ifa->ifa_next) {
            if (ifa->ifa_addr == nullptr) {
                continue;
            }
            if (ifa->ifa_addr->sa_family == AF_INET) {
                struct sockaddr_in *addr = (struct sockaddr_in *) ifa->ifa_addr;
                char ipstr[INET_ADDRSTRLEN];
                inet_ntop(AF_INET, &addr->sin_addr, ipstr, INET_ADDRSTRLEN);
                if (strcmp(ipstr, "127.0.0.1") != 0) {
                    ips.push_back(ipstr);
                }
            }
        }
        freeifaddrs(ifaddr);
        return ips;
    }


    bool NetworkUtils::isIPValid(const std::string &ip) {
        struct sockaddr_in sa;
        return inet_pton(AF_INET, ip.c_str(), &(sa.sin_addr)) != 0;
    }


    int NetworkUtils::createUDPSocket() {
        return socket(AF_INET, SOCK_DGRAM, 0);
    }

    int NetworkUtils::createTCPSocket() {
        return socket(AF_INET, SOCK_STREAM, 0);
    }

    bool NetworkUtils::setSocketNonBlocking(int sockfd) {
        int flags = fcntl(sockfd, F_GETFL, 0);
        if (flags == -1) {
            return false;
        }

        return fcntl(sockfd, F_SETFL, flags | O_NONBLOCK) != -1;
    }

    bool NetworkUtils::setSocketReusable(int sockfd) {
        int enable = 1;
        return setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &enable, sizeof(enable)) == 0;
    }

    bool NetworkUtils::bindSocket(int sockfd, const std::string &ip, int port) {
        struct sockaddr_in addr;
        memset(&addr, 0, sizeof(addr));

        addr.sin_family = AF_INET;
        addr.sin_port = htons(port);

        if (ip.empty() || ip == "0.0.0.0") {
            addr.sin_addr.s_addr = INADDR_ANY;
        } else {
            inet_pton(AF_INET, ip.c_str(), &addr.sin_addr);
        }

        return bind(sockfd, (struct sockaddr *) &addr, sizeof(addr)) == 0;
    }
}