package cn.tenfell.webos.common.util;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class FrameStartUtil {
    public static byte[] logo;
    private static String contact = "暂无联系方式";
    private static String softName = "开源软件";
    private static String systemName = "腾飞Webos社区版";//

    public static void pringInfo() {
        try {
            startFrame();
        } catch (Exception e) {
            try {
                FileUtil.writeUtf8String(ExceptionUtil.stacktraceToString(e), "startError.txt");
            } catch (Exception e2) {
            }
        }
        certificatePringInfo();
        boolean isWin = System.getProperty("os.name").toLowerCase().indexOf("window") != -1;
        if (isWin) {
            desktopKjfs();
        }
    }

    private static void desktopKjfs() {
        String iconPath = ProjectUtil.rootPath + "/webos.ico";
        Map<String, Image> data = iconMap();
        Image img = data.get("i64");
        if (img == null) {
            img = data.get("i32");
        }
        convertPngToIco(img, iconPath);
        createShortcut(ProjectUtil.rootPath + "/auto-restart.bat", iconPath, "腾飞Webos社区版服务端");
    }

    public static void convertPngToIco(Image img, String icoPath) {
        try {
            BufferedImage bimg = ImgUtil.toBufferedImage(img);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bimg, "png", baos);
            byte[] pngBytes = baos.toByteArray();
            ByteBuffer bb = ByteBuffer.allocate(6 + 16 + pngBytes.length);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putShort((short) 0);       // 保留字段，必须为0
            bb.putShort((short) 1);       // 图像类型，1 = 图标
            bb.putShort((short) 1);       // 图像数量，1 为单图像 ICO
            bb.put((byte) bimg.getWidth());     // 宽度，0 表示 256 像素
            bb.put((byte) bimg.getHeight());    // 高度，0 表示 256 像素
            bb.put((byte) 0);                    // 颜色数，0 表示无调色板
            bb.put((byte) 0);                    // 保留字段
            bb.putShort((short) 1);              // 颜色平面
            bb.putShort((short) 32);             // 每像素位数
            bb.putInt(pngBytes.length);          // 图像数据大小
            bb.putInt(6 + 16);                   // 图像数据偏移
            bb.put(pngBytes);
            try (RandomAccessFile raf = new RandomAccessFile(icoPath, "rw");
                FileChannel fc = raf.getChannel()) {
                ((Buffer)bb).flip();
                fc.write(bb);
            }
        } catch (Exception e) {

        }
    }

    private static void createShortcut(String scriptPath, String iconPath, String shortcutName) {
        String vbscriptPath = ProjectUtil.rootPath + "/create-desktop.vbs";
        String vbscript = "Set oWS = WScript.CreateObject(\"WScript.Shell\")\n" +
                "strDesktop = oWS.SpecialFolders(\"Desktop\")\n" +
                "set oLink = oWS.CreateShortcut(strDesktop & \"\\" + shortcutName + ".lnk\")\n" +
                "oLink.TargetPath = \"" + scriptPath + "\"\n" +
                "oLink.IconLocation = \"" + iconPath + ",0\"\n" +
                "oLink.Save\n" +
                "Set oLink = Nothing\n" +
                "Set oWS = Nothing";
        FileUtil.writeString(vbscript, vbscriptPath, CharsetUtil.CHARSET_GBK);
        try {
            RuntimeUtil.exec("wscript", "\"" + vbscriptPath + "\"").waitFor();
        } catch (InterruptedException e) {
        }
        //FileUtil.del(vbscriptPath);
    }

    private static void browserUrl(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                URI uri = new URI(url);
                Desktop desktop = Desktop.getDesktop();
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void openDir(String dir) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(new File(dir));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static Map<String, Image> iconMapData;

    private synchronized static Map<String, Image> iconMap() {
        if (iconMapData != null) {
            return iconMapData;
        }
        iconMapData = new HashMap<>();
        try {
            if (logo == null) {
                try {
                    logo = IoUtil.readBytes(FrameStartUtil.class.getResource("/logo.png").openStream());
                } catch (Exception e) {
                }
            }
            if (logo == null) {
                logo = new byte[0];
            }
            Color color = new Color(0, 0, 0, 0);
            InputStream in = new ByteArrayInputStream(logo);
            Image i64 = ImgUtil.scale(ImgUtil.read(in), 128, 128, color);
            Image i32 = ImgUtil.scale(i64, 32, 32, color);
            Image i16 = ImgUtil.scale(i32, 16, 16, color);
            iconMapData.put("i64", i64);
            iconMapData.put("i32", i32);
            iconMapData.put("i16", i16);
            return iconMapData;
        } catch (Exception e) {
            iconMapData.put("i32", new BufferedImage(32, 32, 1));
            iconMapData.put("i16", new BufferedImage(16, 16, 1));
            return iconMapData;
        } finally {
            logo = null;
        }
    }

    private static void startFrame() {
        JFrame win = new JFrame();
        win.setSize(400, 300);
        win.setLocationRelativeTo(null);
        win.setVisible(true);
        win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        win.setResizable(false);
        win.setTitle(systemName);
        win.setLayout(new GridLayout(3, 1));
        JLabel hy = new JLabel("欢迎使用" + softName + ",客户服务联系" + contact, JLabel.CENTER);
        win.add(hy);
        JLabel portError = new JLabel("正在探测端口...", JLabel.CENTER);
        win.add(portError);
        int port = ProjectUtil.startConfig.getInt("port");
        JPanel jPanel = new JPanel();
        JButton acBtn = new JButton("打开网站首页");
        acBtn.setSize(100, 30);
        jPanel.add(acBtn);
        acBtn.addActionListener(e -> {
            String localIp = "127.0.0.1";
            browserUrl("http://" + localIp + ":" + port);
        });
        JButton ycBtn = new JButton("隐藏到托盘区");
        ycBtn.setSize(100, 30);
        jPanel.add(ycBtn);
        ycBtn.addActionListener(e -> win.setVisible(false));
        win.add(jPanel);
        try {
            boolean isWin = System.getProperty("os.name").toLowerCase().indexOf("window") != -1;
            String path = new File(ProjectUtil.rootPath).getParent();
            PopupMenu pm = new PopupMenu();
            MenuItem m1 = new MenuItem(isWin ? "Show" : "显示");
            m1.addActionListener(e -> {
                win.setVisible(true);
                win.setAlwaysOnTop(true);
            });
            MenuItem m3 = new MenuItem(isWin ? "Open Install Directory" : "打开安装目录");
            m3.addActionListener(e -> {
                openDir(path);
            });
            MenuItem m4 = new MenuItem(isWin ? "Restart Service" : "重启服务");
            m4.addActionListener(e -> {
                ProjectUtil.restartServer();
            });
            MenuItem m2 = new MenuItem(isWin ? "Exit" : "退出");
            m2.addActionListener(e -> System.exit(0));
            pm.add(m1);
            pm.add(m3);
            pm.addSeparator();
            pm.add(m4);
            pm.add(m2);
            Map<String, Image> iconMapData = iconMap();
            SystemTray systemTray = SystemTray.getSystemTray();
            win.setIconImage(iconMapData.get("i32"));
            TrayIcon trayIcon = new TrayIcon(iconMapData.get("i16"), systemName, pm);
            systemTray.add(trayIcon);
            trayIcon.addActionListener(e -> {
                win.setVisible(true);
                win.setAlwaysOnTop(true);
            });
        } catch (Exception e) {
        }
        boolean use = isPortUsing(port);
        portError.setText(use ? (port + "端口已被占用") : (port + "端口未被占用"));
        if (!use) {
            ThreadUtil.execute(() -> {
                while (true) {
                    try {
                        ThreadUtil.sleep(2000);
                        JSONObject res = JSONUtil.parseObj(HttpUtil.get("http://127.0.0.1:" + port + "/webos/api"));
                        if (res.getInt("code", -1) >= 0) {
                            portError.setText(port + "服务正常启动");
                            break;
                        }
                    } catch (Exception e) {
                    }
                }
            });
        }
    }

    private static boolean isPortUsing(int port) {
        boolean flag = false;
        try {
            ServerSocket ss = new ServerSocket(port);
            ss.close();
        } catch (Exception e) {
            flag = true;
        }
        return flag;
    }

    private static void certificatePringInfo() {
        String sysName = System.getProperty("os.name");
        if (sysName.contains("Windows")) {
            System.out.println();
            System.out.println("=========================================================");
            System.out.println("                                                         ");
            System.out.println("        欢迎使用" + softName + ",客户服务联系" + contact + "         ");
            System.out.println("                                                         ");
            System.out.println("=========================================================");
            System.out.println();
        } else {
            String[] color = new String[]{"\u001b[31m", "\u001b[32m", "\u001b[33m", "\u001b[34m", "\u001b[35m",
                    "\u001b[36m", "\u001b[90m", "\u001b[92m", "\u001b[93m", "\u001b[94m", "\u001b[95m", "\u001b[96m"};
            System.out.println();

            int i;
            for (i = 0; i < 57; ++i) {
                System.out.print(color[i % color.length] + "=\u001b[0m");
            }

            System.out.println();
            System.out.println("                                                         ");
            System.out.println("        \u001b[31m欢迎\u001b[92m使用\u001b[95m" + softName
                    + "\u001b[0m,\u001b[37m客户\u001b[0m服务\u001b[91m联系\u001b[93m" + contact + "         ");
            System.out.println("                                                         ");
            for (i = 56; i >= 0; --i) {
                System.out.print(color[i % color.length] + "=\u001b[0m");
            }
            System.out.println();
            System.out.println();
        }
    }
}
