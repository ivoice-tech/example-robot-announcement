/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.server.bootstrap;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import org.apache.logging.log4j.Logger;
import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.plugins.bootstrap.basic.BasicBootstrap;
import org.jboss.kernel.plugins.deployment.xml.BasicXMLDeployer;
import org.jboss.util.StringPropertyReplacer;

import java.io.File;
import java.net.URL;

/**
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 * @author <a href="mailto:amit.bhayani@jboss.com">amit bhayani</a>
 */
public class Main {

    private final static String HOME_DIR = "MMS_HOME";
    private final static String BOOT_URL = "/conf/bootstrap-beans.xml";
    public static final String MMS_HOME = "mms.home.dir";
    public static final String MMS_MEDIA = "mms.media.dir";
    public static final String MMS_BIND_ADDRESS = "mms.bind.address";
    private static final String LINKSET_PERSIST_DIR_KEY = "linkset.persist.dir";
    private static int index = 0;
    private Kernel kernel;
    private BasicXMLDeployer kernelDeployer;
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(Main.class);

    public static void main(String[] args) throws Throwable {
        String homeDir = getHomeDir(args);
        System.setProperty(MMS_HOME, homeDir);
        System.setProperty(MMS_MEDIA, homeDir + File.separator + "media" + File.separator);
        System.out.println("Running media server with args");
        for (String arg : args) {
            System.out.println("ARG: " + arg);
        }
        System.out.println("MMS HOME DIR: " + System.getProperty(MMS_HOME));
        System.out.println("MMS MEDIA DIR: " + System.getProperty(MMS_MEDIA));

        //This is for SS7 configuration file persistence
        System.setProperty(LINKSET_PERSIST_DIR_KEY, homeDir + File.separator + "ss7");

        logger.info("Home directory: " + homeDir);

        URL bootURL = getURL("${" + MMS_HOME + "}" + BOOT_URL);
        Main main = new Main();

        main.processCommandLine(args);
        logger.info("Booting from " + bootURL);
        main.boot(bootURL);
    }

    private void processCommandLine(String[] args) {

        String programName = System.getProperty("program.name", "Mobicents Media Server");

        int c;
        String arg;
        LongOpt[] longopts = new LongOpt[2];
        longopts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
        longopts[1] = new LongOpt("host", LongOpt.REQUIRED_ARGUMENT, null, 'b');

        Getopt g = new Getopt("MMS", args, "-:b:h", longopts);
        g.setOpterr(false); // We'll do our own error handling
        //
        while ((c = g.getopt()) != -1) {
            switch (c) {

                //
                case 'b':
                    arg = g.getOptarg();
                    System.setProperty(MMS_BIND_ADDRESS, arg);

                    break;
                //

                case 'h':
                    System.out.println("usage: " + programName + " [options]");
                    System.out.println();
                    System.out.println("options:");
                    System.out.println("    -h, --help                    Show this help message");
                    System.out.println("    -b, --host=<host or ip>       Bind address for all Mobicents Media Server services");
                    System.out.println();
                    System.exit(0);
                    break;

                case ':':
                    System.out.println("You need an argument for option " + (char) g.getOptopt());
                    System.exit(0);
                    break;
                //
                case '?':
                    System.out.println("The option '" + (char) g.getOptopt() + "' is not valid");
                    System.exit(0);
                    break;
                //
                default:
                    System.out.println("getopt() returned " + c);
                    break;
            }
        }

        if (System.getProperty(MMS_BIND_ADDRESS) == null) {
            System.setProperty(MMS_BIND_ADDRESS, "127.0.0.1");
        }

    }


    /**
     * Gets the Media Server Home directory.
     *
     * @param args the command line arguments
     * @return the path to the home directory.
     */
    private static String getHomeDir(String[] args) {
        String mmsHomeDir = System.getProperty(HOME_DIR);
        if (mmsHomeDir == null) {
            mmsHomeDir = System.getenv(HOME_DIR);
            if (mmsHomeDir == null) {
                if (args.length > index) {
                    mmsHomeDir = args[index++];
                } else {
                    mmsHomeDir = ".";
                }
            }
        }
        return mmsHomeDir;
    }

    protected void boot(URL bootURL) throws Throwable {
        BasicBootstrap bootstrap = new BasicBootstrap();
        bootstrap.run();

        // register shutdown thread
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownThread()));

        kernel = bootstrap.getKernel();
        kernelDeployer = new BasicXMLDeployer(kernel);


        kernelDeployer.deploy(bootURL);
        kernelDeployer.validate();

        Controller controller = kernel.getController();

        ControllerContext context = controller.getInstalledContext("MainDeployer");
        if (context != null) {
            MainDeployer deployer = (MainDeployer) context.getTarget();
            deployer.start(kernel, kernelDeployer);
        }
    }

    public static URL getURL(String filePath) throws Exception {
        // replace ${} inputs
        filePath = StringPropertyReplacer.replaceProperties(filePath, System.getProperties());
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("No such file: " + filePath);
        }
        return file.toURI().toURL();
    }

    private class ShutdownThread implements Runnable {
        public void run() {
            System.out.println("Shutting down");
            kernelDeployer.shutdown();
            kernelDeployer = null;

            kernel.getController().shutdown();
            kernel = null;
        }
    }
}
