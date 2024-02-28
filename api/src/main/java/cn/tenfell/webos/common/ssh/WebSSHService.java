package cn.tenfell.webos.common.ssh;

import org.noear.solon.core.message.Session;

import java.io.IOException;

/**
 * @Description: WebSSH的业务逻辑
 * @Author: NoCortY
 * @Date: 2020/3/7
 */
public interface WebSSHService {
    /**
     * @Description: 初始化ssh连接
     * @Param:
     * @return:
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    public void initConnection(Session session);

    /**
     * @Description: 处理客户段发的数据
     * @Param:
     * @return:
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    public void recvHandle(String buffer, Session session);

    /**
     * @Description: 数据写回前端 for websocket
     * @Param:
     * @return:
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    public void sendMessage(Session session, byte[] buffer) throws IOException;

    /**
     * @Description: 关闭连接
     * @Param:
     * @return:
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    public void close(Session session);
}
