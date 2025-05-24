//
// Created by 64860 on 2025/5/6.
//

#include "message_protocol.h"
#include <cstring>
#include <arpa/inet.h>

namespace p2p {

    vector<uint8_t>
    MessageProtocol::serializeDiscoveryMessage(const std::string &local_ip, int port) {
        vector<uint8_t> data;
        data.reserve(1 + 4 + local_ip.size() + 4);

        //消息类型
        data.push_back(DISCOVERY);

        //IP地址长度和内容
        uint32_t ip_len = htonl(local_ip.size());
        data.insert(data.end(), reinterpret_cast<uint8_t *>(&ip_len), reinterpret_cast<uint8_t *>(&ip_len) + 4);
        data.insert(data.end(), local_ip.begin(), local_ip.end());

        //端口号
        uint32_t net_port = htonl(port);
        data.insert(data.end(), reinterpret_cast<uint8_t *>(&net_port),
                    reinterpret_cast<uint8_t *>(&net_port) + 4);
        return data;
    }


    bool MessageProtocol::parseDiscoveryMessage(const vector<uint8_t> &data, std::string &ip,
                                                int &port) {
        if (data.size() < 9 || data[0] != DISCOVERY) {
            return false;
        }

        //解析IP长度
        uint32_t ip_len;
        memcpy(&ip_len, &data[1], 4);
        ip_len = ntohl(ip_len);

        if (data.size() < 9 + ip_len) {
            return false;
        }

        //解析IP
        ip.assign(reinterpret_cast<const char *>(&data[5]), ip_len);

        //解析端口号
        uint32_t net_port;
        memcpy(&net_port, &data[5 + ip_len], 4);
        port = ntohl(net_port);

        return true;
    }


    vector<uint8_t> MessageProtocol::serializeDataMessage(const std::string &data) {
        vector<uint8_t> result;
        result.reserve(5 + data.size());

        //消息类型
        result.push_back(DATA);

        //数据长度
        uint32_t len = htonl(data.size());
        result.insert(result.end(), reinterpret_cast<uint8_t *>(&len),
                      reinterpret_cast<uint8_t *>(&len) + 4);

        //数据内容
        result.insert(result.end(), data.begin(), data.end());

        return result;
    }


    bool MessageProtocol::parseDataMessage(const vector<uint8_t> &data, std::string &content) {
        if (data.size() < 5 || data[0] != DATA) {
            return false;
        }

        //解析长度
        uint32_t len;
        memcpy(&len, &data[1], 4);
        len = ntohl(len);

        if (data.size() < 5 + len) {
            return false;
        }

        //解析内容
        content.assign(reinterpret_cast<const char *>(&data[5]), len);

        return true;
    }


    vector<uint8_t> MessageProtocol::serializeHeartbeatMessage() {
        vector<uint8_t> result;
        result.push_back(HEARTBEAT);
        return result;
    }

    bool MessageProtocol::isHeartbeatMessage(const vector<uint8_t> &data) {
        return !data.empty() && data[0] == HEARTBEAT;
    }

}