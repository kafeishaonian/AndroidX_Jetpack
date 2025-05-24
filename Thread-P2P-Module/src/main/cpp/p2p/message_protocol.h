//
// Created by 64860 on 2025/5/6.
//

#ifndef ANDROIDX_JETPACK_MESSAGE_PROTOCOL_H
#define ANDROIDX_JETPACK_MESSAGE_PROTOCOL_H

#include <vector>
#include <string>

using namespace std;

namespace p2p {

    class MessageProtocol{

        enum MessageType {
            DISCOVERY = 0x01,
            CONNECTION = 0x02,
            DATA = 0x03,
            HEARTBEAT = 0x04
        };

    public:
        static vector<uint8_t> serializeDiscoveryMessage(const string& local_ip, int port);
        static bool parseDiscoveryMessage(const vector<uint8_t>& data, string& ip, int& prot);

        static vector<uint8_t> serializeDataMessage(const string& data);
        static bool parseDataMessage(const vector<uint8_t>& data, string& content);

        static vector<uint8_t> serializeHeartbeatMessage();
        static bool isHeartbeatMessage(const vector<uint8_t>& data);

    };
}


#endif //ANDROIDX_JETPACK_MESSAGE_PROTOCOL_H
