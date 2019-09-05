package snowflake.components.files.browser.ssh;

import snowflake.common.FileInfo;
import snowflake.common.FileSystem;
import snowflake.common.local.files.LocalFileSystem;
import snowflake.common.ssh.SshClient;
import snowflake.common.ssh.files.SshFileSystem;
import snowflake.components.files.*;
import snowflake.components.files.browser.AbstractFileBrowserView;
import snowflake.components.files.browser.AddressBar;
import snowflake.components.files.browser.FileBrowser;
import snowflake.utils.PathUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SftpFileBrowserView extends AbstractFileBrowserView {
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private SshMenuHandler menuHandler;
    private FileBrowser fileBrowser;
    private DndTransferHandler transferHandler;

    public SftpFileBrowserView(FileBrowser fileBrowser,
                               JRootPane rootPane, FileComponentHolder holder) {
        super(rootPane, holder);
        this.fileBrowser = fileBrowser;
        this.menuHandler = new SshMenuHandler(fileBrowser, this, holder);
        this.menuHandler.initMenuHandler(this.folderView);
        this.transferHandler = new DndTransferHandler(this.folderView, holder.getInfo(), this);
        this.folderView.setTransferHandler(transferHandler);
        this.folderView.setFolderViewTransferHandler(transferHandler);
        this.path = holder.getInfo().getRemoteFolder();
        this.render(path);
    }


    private void openDefaultAction() {
    }

    private void openNewTab() {

    }

    public void createAddressBar() {
        addressBar = new AddressBar('/', new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("clicked");
            }
        });
    }

    @Override
    public String toString() {
        return holder.getInfo().getName() + " [" + this.path + "]";
    }

//    private void connect() throws Exception {
//        synchronized (fileSystemMap) {
//            fs = fileSystemMap.get(source.getInfo());
//            if (fs == null || !fs.isConnected()) {
//                if (fs == null) {
//                    fs = new SshFileSystem(source);
//                }
//                try {
//                    fs.connect();
//                    fileSystemMap.put(source.getInfo(), fs);
//                    fileViewMap.put(fs, 1);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            } else {
//                fileViewMap.put(fs, fileViewMap.get(fs) + 1);
//            }
//
//            String home = source.getInfo().getRemoteFolder();
//            if (home == null) {
//                home = fs.getHome();
//            }
//            this.path = home;
//            final String finalHome = home;
//            SwingUtilities.invokeLater(() -> {
//                addressBar.setText(finalHome);
//            });
//        }
//    }

    private void renderDirectory(final String path) throws Exception {
        final List<FileInfo> list = holder.getSshFileSystem().list(path);
        SwingUtilities.invokeLater(() -> {
            addressBar.setText(path);
            folderView.setItems(list);
        });
    }

    @Override
    public void addBack(String path) {

    }

    @Override
    public void render(String path) {
        this.path = path;
        executor.submit(() -> {
            this.fileBrowser.disableUi();
            while (true) {
                try {
                    if (path == null) {
                        this.path = holder.getSshFileSystem().getHome();
                    }
                    renderDirectory(this.path);
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    if (JOptionPane.showConfirmDialog(null,
                            "Unable to connect to server " + holder.getInfo().getName() + " at " + holder.getInfo().getHost() +
                                    "\nDo you want to retry?") == JOptionPane.YES_OPTION) {
                        continue;
                    }
                    break;
                }
            }
            this.fileBrowser.enableUi();
        });
    }

    @Override
    public void openApp(FileInfo file) {

    }


    protected void up() {
        String parent = PathUtils.getParent(path);
        addBack(path);
        render(parent);
    }

    protected void home() {
        addBack(path);
        render(null);
    }

    @Override
    public void install(JComponent c) {

    }

    @Override
    public boolean createMenu(JPopupMenu popup, FileInfo[] files) {
        return menuHandler.createMenu(popup, files);
    }

    public boolean handleDrop(DndTransferData transferData) {
        System.out.println("Dropped: " + transferData);
        int sessionHashCode = transferData.getInfo();
        FileSystem sourceFs = null;
        if (sessionHashCode == 0) {
            sourceFs = new LocalFileSystem();
        }
        if (sourceFs instanceof LocalFileSystem) {
            FileSystem targetFs = holder.getSshFileSystem();
            holder.newFileTransfer(sourceFs, targetFs, transferData.getFiles(), transferData.getCurrentDirectory(), this.path, this.hashCode());
        }
        return true;
    }

    public FileSystem getFileSystem() {
        return this.holder.getSshFileSystem();
    }

    public SshClient getSshClient() {
        return ((SshFileSystem) this.holder.getSshFileSystem()).getWrapper();
    }
}