//
// Created by 64860 on 2025/5/6.
//

#ifndef ANDROIDX_JETPACK_NETWORK_UTILS_H
#define ANDROIDX_JETPACK_NETWORK_UTILS_H

#include <arpa/inet.h>
#include <string>
#include <vector>

using namespace std;

namespace p2p {
    class NetworkUtils {

    public:
        static string getLocalIP();

        static vector<string> getAllIPs();

        static bool isIPValid(const string &ip);

        static int createUDPSocket();

        static int createTCPSocket();

        static bool setSocketNonBlocking(int sockfd);

        static bool setSocketReusable(int sockfd);

        static bool bindSocket(int sockfd, const string &ip, int port);

    };
}


#endif //ANDROIDX_JETPACK_NETWORK_UTILS_H
