package cn.tenfell.webos.common.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import lombok.Data;
import org.noear.solon.core.message.Session;
/**
* @Description: ssh连接信息
* @Author: NoCortY
* @Date: 2020/3/8
*/
@Data
public class SSHConnectInfo {
    private Session webSocketSession;
    private JSch jSch;
    private Channel channel;
}
