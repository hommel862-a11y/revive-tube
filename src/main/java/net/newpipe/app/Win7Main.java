package net.newpipe.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.VideoStream;

/**
 * YouTube Lite para Windows 7 / Canaima x86.
 * Diseñado y optimizado para arquitecturas Atom N455 / Canaima con bajo consumo de RAM y CPU.
 */
public final class Win7Main extends JFrame {
    private static final int MAX_RESULTS = 30;
    private static final int MAX_RECENT_SEARCHES = 5;
    private static final int MAX_RECENT_CHANNELS = 12;
    private static final int THUMBNAIL_WIDTH = 120;
    private static final int THUMBNAIL_HEIGHT = 68;
    private static final int MAX_THUMBNAIL_BYTES = 0x200000;
    private static final int MAX_THUMBNAIL_CACHE_SIZE = 50;
    private static final int DEFAULT_VIDEO_HEIGHT = 360; // 360p ideal para Atom N455 / Canaima
    private static final Pattern HEIGHT_PATTERN = Pattern.compile("(?:^|x)([0-9]{3,4})(?:p(?:[0-9]+)?|$)");
    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile("(?:youtu\\.be/|/shorts/|/embed/|[?&]v=)([A-Za-z0-9_-]{11})");
    private static final String VLC_PREFERENCE_KEY = "vlc.executable";
    private static final String RECENT_SEARCH_COUNT_KEY = "recent.search.count";
    private static final String RECENT_SEARCH_KEY_PREFIX = "recent.search.";
    private static final String RECENT_CHANNEL_COUNT_KEY = "recent.channel.count";
    private static final String RECENT_CHANNEL_KEY_PREFIX = "recent.channel.";
    private static final String THEME_PREFERENCE_KEY = "ui.theme";
    private static final String RESOLUTION_PREFERENCE_KEY = "video.max.resolution";
    private static final String AUDIO_ONLY_PREFERENCE_KEY = "audio.only.mode";
    private static final int[] VIDEO_RESOLUTIONS = new int[]{144, 240, 360, 480};

    private static Theme currentTheme = Theme.DARK;
    private static int selectedMaxResolution = 360;
    private static boolean audioOnlyMode = false;
    private static Color HEADER_COLOR = Theme.DARK.header;
    private static Color PAGE_COLOR = Theme.DARK.page;
    private static Color CARD_COLOR = Theme.DARK.card;
    private static Color TEXT_COLOR = Theme.DARK.text;
    private static Color MUTED_COLOR = Theme.DARK.muted;
    private static Color BORDER_COLOR = Theme.DARK.border;
    private static Color ACCENT_COLOR = Theme.DARK.accent;
    private static Color ACCENT_LIGHT_COLOR = Theme.DARK.accentLight;
    private static BufferedImage thumbnailPlaceholder = createThumbnailPlaceholder();
    private static final StreamingService YOUTUBE = ServiceList.YouTube;

    private final DefaultListModel<StreamInfoItem> resultModel = new DefaultListModel<>();
    private final JList<StreamInfoItem> resultList = new JList<>(this.resultModel);
    private final JTextField searchField = new JTextField();
    private final JButton searchButton = new ModernButton("Buscar", true);
    private final JButton loadMoreButton = new ModernButton("Cargar más videos", false);
    private final JButton playButton = new ModernButton("▶ Reproducir", true);
    private final JButton viewChannelButton = new ModernButton("Canal", false);
    private final JButton backToResultsButton = new ModernButton("◀ Volver", false);
    private final JButton homeButton = new ModernButton("🏠 Inicio", false);
    private final JCheckBox audioOnlyCheckBox = new JCheckBox("🎧 Solo Audio");
    private final JComboBox<Integer> resolutionComboBox = new JComboBox<>();
    private final JLabel statusLabel = new JLabel("Listo");
    private final JLabel sectionTitle = new JLabel("TENDENCIAS EN YOUTUBE");
    private final JPanel channelRecommendationsPanel = new JPanel(new BorderLayout(0, 4));
    private final JPanel channelButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
    private final JPanel detailsPanel = new JPanel(new BorderLayout(8, 0));
    private final JLabel videoDetailsLabel = new JLabel("Selecciona un video para reproducir.");

    private final ConcurrentHashMap<String, StreamInfo> infoCache = new ConcurrentHashMap<>();
    private final Set<String> thumbnailLoads = ConcurrentHashMap.newKeySet();
    private final Set<String> failedThumbnailUrls = ConcurrentHashMap.newKeySet();
    private final Map<String, BufferedImage> thumbnailCache = new LinkedHashMap<String, BufferedImage>(50, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return this.size() > MAX_THUMBNAIL_CACHE_SIZE;
        }
    };
    private final ThreadPoolExecutor thumbnailExecutor = new ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(24),
        new ThumbnailThreadFactory(),
        new ThreadPoolExecutor.DiscardOldestPolicy()
    );
    private final Preferences preferences = Preferences.userNodeForPackage(Win7Main.class);
    private Path vlcExecutable = this.loadVlcExecutable();
    private Process activePlayerProcess;
    private Page nextPage;
    private SearchQueryHandler activeSearchHandler;
    private ChannelTabExtractor activeChannelExtractor;
    private String activeChannelName;
    private boolean recommendationsMode;
    private List<StreamInfoItem> previousResults;
    private Page previousNextPage;
    private SearchQueryHandler previousSearchHandler;
    private ChannelTabExtractor previousChannelExtractor;
    private String previousChannelName;
    private boolean previousRecommendationsMode;

    public Win7Main() {
        super("YouTube Lite para Windows 7 / Canaima");
        NewPipe.init(new DesktopDownloader());
        this.buildWindow();
    }

    private static void loadAppearancePreferences() {
        Preferences appearance = Preferences.userNodeForPackage(Win7Main.class);
        currentTheme = Theme.fromId(appearance.get(THEME_PREFERENCE_KEY, Theme.DARK.id));
        selectedMaxResolution = normalizeResolution(appearance.getInt(RESOLUTION_PREFERENCE_KEY, DEFAULT_VIDEO_HEIGHT));
        audioOnlyMode = appearance.getBoolean(AUDIO_ONLY_PREFERENCE_KEY, false);

        HEADER_COLOR = currentTheme.header;
        PAGE_COLOR = currentTheme.page;
        CARD_COLOR = currentTheme.card;
        TEXT_COLOR = currentTheme.text;
        MUTED_COLOR = currentTheme.muted;
        BORDER_COLOR = currentTheme.border;
        ACCENT_COLOR = currentTheme.accent;
        ACCENT_LIGHT_COLOR = currentTheme.accentLight;
        thumbnailPlaceholder = createThumbnailPlaceholder();
    }

    private static int normalizeResolution(int resolution) {
        for (int supported : VIDEO_RESOLUTIONS) {
            if (supported == resolution) return supported;
        }
        return DEFAULT_VIDEO_HEIGHT;
    }

    public static void main(String[] args) {
        SslHelper.installPermissiveSsl();
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception exception) {
                System.err.println("No se pudo cargar tema nativo: " + exception.getMessage());
            }
            loadAppearancePreferences();
            configureTheme();
            Win7Main window = new Win7Main();
            window.setVisible(true);
            window.loadHomePage();
        });
    }

    private static void configureTheme() {
        UIManager.put("Panel.background", PAGE_COLOR);
        UIManager.put("Label.foreground", TEXT_COLOR);
        UIManager.put("TextField.background", CARD_COLOR);
        UIManager.put("TextField.foreground", TEXT_COLOR);
        UIManager.put("TextField.caretForeground", TEXT_COLOR);
        UIManager.put("CheckBox.background", HEADER_COLOR);
        UIManager.put("CheckBox.foreground", Color.WHITE);
        UIManager.put("ComboBox.background", CARD_COLOR);
        UIManager.put("ComboBox.foreground", TEXT_COLOR);
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 12));
    }

    private void showSettingsDialog() {
        JComboBox<Theme> themeSelector = new JComboBox<>(Theme.values());
        themeSelector.setSelectedItem(currentTheme);

        JComboBox<Integer> resSelector = new JComboBox<>();
        for (int res : VIDEO_RESOLUTIONS) {
            resSelector.addItem(res);
        }
        resSelector.setSelectedItem(selectedMaxResolution);

        JPanel settings = new JPanel(new GridLayout(3, 2, 8, 8));
        settings.add(new JLabel("Tema visual:"));
        settings.add(themeSelector);
        settings.add(new JLabel("Resolución máxima (360p ideal Canaima):"));
        settings.add(resSelector);
        settings.add(new JLabel("Ruta de VLC:"));
        JButton vlcBtn = new JButton(vlcExecutable == null ? "Elegir VLC..." : vlcExecutable.getFileName().toString());
        vlcBtn.addActionListener(e -> {
            if (chooseVlcExecutable()) {
                vlcBtn.setText(vlcExecutable.getFileName().toString());
            }
        });
        settings.add(vlcBtn);

        int result = JOptionPane.showConfirmDialog(this, settings, "Configuración del Sistema", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            Theme selTheme = (Theme) themeSelector.getSelectedItem();
            Integer selRes = (Integer) resSelector.getSelectedItem();
            if (selTheme != null && selRes != null) {
                this.preferences.put(THEME_PREFERENCE_KEY, selTheme.id);
                this.preferences.putInt(RESOLUTION_PREFERENCE_KEY, selRes);
                selectedMaxResolution = selRes;
                this.resolutionComboBox.setSelectedItem(selRes);
                if (selTheme != currentTheme) {
                    this.dispose();
                    loadAppearancePreferences();
                    configureTheme();
                    Win7Main replacement = new Win7Main();
                    replacement.setVisible(true);
                    replacement.loadHomePage();
                }
            }
        }
    }

    private void buildWindow() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                cleanupAndExit();
            }

            @Override
            public void windowClosed(WindowEvent event) {
                cleanupAndExit();
            }
        });

        this.setMinimumSize(new Dimension(720, 480));
        this.setSize(960, 560);
        this.setLocationRelativeTo(null);
        this.getContentPane().setBackground(PAGE_COLOR);

        // Header ultra-compacto (38px de alto)
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(HEADER_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel navLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        navLeft.setOpaque(false);
        styleButton(this.homeButton, false);
        this.homeButton.addActionListener(e -> this.loadHomePage());
        styleButton(this.backToResultsButton, false);
        this.backToResultsButton.setVisible(false);
        this.backToResultsButton.addActionListener(e -> this.restorePreviousResults());
        navLeft.add(this.homeButton);
        navLeft.add(this.backToResultsButton);

        JPanel searchBox = new JPanel(new BorderLayout(4, 0));
        searchBox.setOpaque(false);
        this.searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.searchField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(BORDER_COLOR, 8),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        this.searchField.setToolTipText("Escribe un tema o video y presiona Enter");
        this.searchField.addActionListener(e -> this.search());

        styleButton(this.searchButton, true);
        this.searchButton.addActionListener(e -> this.search());
        searchBox.add(this.searchField, BorderLayout.CENTER);
        searchBox.add(this.searchButton, BorderLayout.EAST);

        JPanel quickControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        quickControls.setOpaque(false);

        this.audioOnlyCheckBox.setOpaque(false);
        this.audioOnlyCheckBox.setSelected(audioOnlyMode);
        this.audioOnlyCheckBox.setFont(new Font("Segoe UI", Font.BOLD, 11));
        this.audioOnlyCheckBox.setForeground(Color.WHITE);
        this.audioOnlyCheckBox.setToolTipText("Ahorra 90% de CPU en Atom: reproduce solo el audio");
        this.audioOnlyCheckBox.addActionListener(e -> {
            audioOnlyMode = this.audioOnlyCheckBox.isSelected();
            this.preferences.putBoolean(AUDIO_ONLY_PREFERENCE_KEY, audioOnlyMode);
        });

        for (int res : VIDEO_RESOLUTIONS) {
            this.resolutionComboBox.addItem(res);
        }
        this.resolutionComboBox.setSelectedItem(selectedMaxResolution);
        this.resolutionComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        this.resolutionComboBox.setToolTipText("Resolución máxima (360p recomendada para Canaima)");
        this.resolutionComboBox.addActionListener(e -> {
            Integer sel = (Integer) this.resolutionComboBox.getSelectedItem();
            if (sel != null) {
                selectedMaxResolution = sel;
                this.preferences.putInt(RESOLUTION_PREFERENCE_KEY, sel);
            }
        });

        JButton settingsBtn = new ModernButton("⚙", false);
        styleButton(settingsBtn, false);
        settingsBtn.setToolTipText("Configuración y VLC");
        settingsBtn.addActionListener(e -> this.showSettingsDialog());

        quickControls.add(this.audioOnlyCheckBox);
        quickControls.add(this.resolutionComboBox);
        quickControls.add(settingsBtn);

        header.add(navLeft, BorderLayout.WEST);
        header.add(searchBox, BorderLayout.CENTER);
        header.add(quickControls, BorderLayout.EAST);

        this.resultList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        this.resultList.setCellRenderer(new ResultCellRenderer());
        this.resultList.setFixedCellHeight(THUMBNAIL_HEIGHT + 14);
        this.resultList.setBackground(CARD_COLOR);
        this.resultList.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        this.resultList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                this.showSelectedVideo();
            }
        });
        this.resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Win7Main.this.playSelected();
                }
            }
        });
        this.resultList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "play-selected");
        this.resultList.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "play-selected");
        this.resultList.getActionMap().put("play-selected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Win7Main.this.playSelected();
            }
        });

        JScrollPane scrollPane = new JScrollPane(this.resultList);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(CARD_COLOR);

        this.channelRecommendationsPanel.setOpaque(false);
        this.channelButtonsPanel.setOpaque(false);
        JScrollPane channelScroll = new JScrollPane(this.channelButtonsPanel,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        channelScroll.setBorder(BorderFactory.createEmptyBorder());
        channelScroll.setOpaque(false);
        channelScroll.getViewport().setOpaque(false);
        channelScroll.setPreferredSize(new Dimension(800, 36));
        this.channelRecommendationsPanel.add(channelScroll, BorderLayout.CENTER);
        this.channelRecommendationsPanel.setVisible(false);

        JPanel resultsTop = new JPanel(new BorderLayout(0, 4));
        resultsTop.setOpaque(false);
        this.sectionTitle.setForeground(TEXT_COLOR);
        this.sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        resultsTop.add(this.sectionTitle, BorderLayout.NORTH);
        resultsTop.add(this.channelRecommendationsPanel, BorderLayout.CENTER);

        this.loadMoreButton.setVisible(false);
        styleButton(this.loadMoreButton, false);
        this.loadMoreButton.addActionListener(e -> this.loadMoreResults());
        JPanel loadMorePanel = new JPanel();
        loadMorePanel.setOpaque(false);
        loadMorePanel.add(this.loadMoreButton);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 4));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 0, 10));
        centerPanel.add(resultsTop, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(loadMorePanel, BorderLayout.SOUTH);

        // Barra inferior con botón directo ▶ Reproducir (ideal para Canaimas táctiles y clics únicos)
        this.detailsPanel.setOpaque(true);
        this.detailsPanel.setBackground(CARD_COLOR);
        this.detailsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        this.videoDetailsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        this.videoDetailsLabel.setForeground(MUTED_COLOR);

        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionButtonsPanel.setOpaque(false);

        styleButton(this.playButton, true);
        this.playButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        this.playButton.setEnabled(false);
        this.playButton.addActionListener(e -> this.playSelected());

        this.viewChannelButton.setVisible(false);
        styleButton(this.viewChannelButton, false);
        this.viewChannelButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        this.viewChannelButton.addActionListener(e -> this.openSelectedChannel());

        actionButtonsPanel.add(this.playButton);
        actionButtonsPanel.add(this.viewChannelButton);

        this.statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        this.statusLabel.setForeground(TEXT_COLOR);

        this.detailsPanel.add(this.statusLabel, BorderLayout.WEST);
        this.detailsPanel.add(this.videoDetailsLabel, BorderLayout.CENTER);
        this.detailsPanel.add(actionButtonsPanel, BorderLayout.EAST);

        this.setLayout(new BorderLayout());
        this.add(header, BorderLayout.NORTH);
        this.add(centerPanel, BorderLayout.CENTER);
        this.add(this.detailsPanel, BorderLayout.SOUTH);
    }

    private void cleanupAndExit() {
        if (this.activePlayerProcess != null && this.activePlayerProcess.isAlive()) {
            this.activePlayerProcess.destroy();
        }
        this.thumbnailExecutor.shutdownNow();
    }

    private static void styleButton(JButton button, boolean primary) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setForeground(primary ? Color.WHITE : TEXT_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void search() {
        final String query = this.searchField.getText().trim();
        if (query.isEmpty()) {
            this.statusLabel.setText("Escribe algo para buscar.");
            return;
        }
        this.setBusy(true, "Buscando en YouTube...");
        this.infoCache.clear();
        this.nextPage = null;
        this.activeSearchHandler = null;
        this.recommendationsMode = false;
        this.activeChannelExtractor = null;
        this.activeChannelName = null;
        this.previousResults = null;
        this.backToResultsButton.setVisible(false);
        this.channelRecommendationsPanel.setVisible(false);
        this.viewChannelButton.setVisible(false);
        this.playButton.setEnabled(false);
        this.loadMoreButton.setVisible(false);
        this.videoDetailsLabel.setText("Buscando videos...");

        new SwingWorker<List<StreamInfoItem>, Void>() {
            private SearchInfo searchInfo;
            private SearchQueryHandler queryHandler;

            @Override
            protected List<StreamInfoItem> doInBackground() throws Exception {
                this.queryHandler = YOUTUBE.getSearchQHFactory().fromQuery(query);
                this.searchInfo = SearchInfo.getInfo(YOUTUBE, this.queryHandler);
                return Win7Main.streamItems(this.searchInfo.getRelatedItems(), MAX_RESULTS);
            }

            @Override
            protected void done() {
                try {
                    List<StreamInfoItem> results = this.get();
                    Win7Main.this.activeSearchHandler = this.queryHandler;
                    Win7Main.this.nextPage = (this.searchInfo != null) ? this.searchInfo.getNextPage() : null;
                    Win7Main.this.rememberSearch(query);
                    Win7Main.this.resultModel.clear();
                    Win7Main.this.rememberChannels(results);
                    for (StreamInfoItem item : results) {
                        Win7Main.this.resultModel.addElement(item);
                    }
                    Win7Main.this.statusLabel.setText(Win7Main.this.resultModel.isEmpty()
                        ? "Sin resultados para: " + query : Win7Main.this.resultModel.size() + " videos encontrados.");
                    Win7Main.this.sectionTitle.setText("RESULTADOS: " + query);
                    Win7Main.this.videoDetailsLabel.setText(results.isEmpty() ? "No se encontraron videos." : "Selecciona un video de la lista.");
                    Win7Main.this.updateLoadMoreButton();
                } catch (Exception exception) {
                    Win7Main.this.showFailure("Error al buscar en YouTube", exception);
                } finally {
                    Win7Main.this.setBusy(false, Win7Main.this.statusLabel.getText());
                }
            }
        }.execute();
    }

    /**
     * Carga videos de Tendencias en la página de inicio para que nunca esté vacía.
     */
    private void loadTrendingVideos() {
        this.setBusy(true, "Cargando videos populares...");
        this.sectionTitle.setText("TENDENCIAS EN YOUTUBE");
        this.loadMoreButton.setVisible(false);

        new SwingWorker<List<StreamInfoItem>, Void>() {
            @Override
            protected List<StreamInfoItem> doInBackground() throws Exception {
                KioskExtractor kiosk = YOUTUBE.getKioskList().getDefaultKioskExtractor();
                kiosk.fetchPage();
                ListExtractor.InfoItemsPage<InfoItem> page = kiosk.getInitialPage();
                return Win7Main.streamItems(page.getItems(), MAX_RESULTS);
            }

            @Override
            protected void done() {
                try {
                    List<StreamInfoItem> items = this.get();
                    Win7Main.this.resultModel.clear();
                    Win7Main.this.rememberChannels(items);
                    for (StreamInfoItem item : items) {
                        Win7Main.this.resultModel.addElement(item);
                    }
                    Win7Main.this.statusLabel.setText(items.size() + " videos populares cargados.");
                    Win7Main.this.sectionTitle.setText("TENDENCIAS EN YOUTUBE");
                } catch (Exception exception) {
                    Win7Main.this.showFailure("No se pudieron cargar tendencias", exception);
                } finally {
                    Win7Main.this.setBusy(false, Win7Main.this.statusLabel.getText());
                }
            }
        }.execute();
    }

    private void loadRecentRecommendations() {
        final List<String> recentSearches = this.getRecentSearches();
        this.recommendationsMode = true;
        this.activeChannelExtractor = null;
        this.activeChannelName = null;
        this.activeSearchHandler = null;
        this.nextPage = null;
        this.channelRecommendationsPanel.setVisible(true);
        this.refreshChannelRecommendations();

        if (recentSearches.isEmpty()) {
            this.loadTrendingVideos();
            return;
        }

        this.setBusy(true, "Cargando sugerencias...");
        this.loadMoreButton.setVisible(false);
        new SwingWorker<RecommendationResults, Void>() {
            @Override
            protected RecommendationResults doInBackground() throws Exception {
                List<StreamInfoItem> recs = new ArrayList<>();
                Set<String> urls = new LinkedHashSet<>();
                SearchInfo lastSearch = null;
                SearchQueryHandler lastHandler = null;
                int count = Math.min(3, recentSearches.size());
                for (int i = 0; i < count && recs.size() < MAX_RESULTS; i++) {
                    String q = recentSearches.get(i);
                    SearchQueryHandler qh = YOUTUBE.getSearchQHFactory().fromQuery(q);
                    SearchInfo si = SearchInfo.getInfo(YOUTUBE, qh);
                    Win7Main.addUniqueStreams(recs, urls, si.getRelatedItems(), 10);
                    lastSearch = si;
                    lastHandler = qh;
                }
                return new RecommendationResults(recs, lastHandler, lastSearch == null ? null : lastSearch.getNextPage());
            }

            @Override
            protected void done() {
                try {
                    RecommendationResults res = this.get();
                    Win7Main.this.resultModel.clear();
                    if (res.items.isEmpty()) {
                        Win7Main.this.loadTrendingVideos();
                        return;
                    }
                    Win7Main.this.rememberChannels(res.items);
                    for (StreamInfoItem item : res.items) {
                        Win7Main.this.resultModel.addElement(item);
                    }
                    Win7Main.this.activeSearchHandler = res.queryHandler;
                    Win7Main.this.nextPage = res.nextPage;
                    Win7Main.this.recommendationsMode = true;
                    Win7Main.this.statusLabel.setText(res.items.size() + " videos recomendados.");
                    Win7Main.this.sectionTitle.setText("RECOMENDADOS PARA TI");
                    Win7Main.this.updateLoadMoreButton();
                    Win7Main.this.refreshChannelRecommendations();
                } catch (Exception exception) {
                    Win7Main.this.showFailure("No se pudieron cargar recomendaciones", exception);
                } finally {
                    Win7Main.this.setBusy(false, Win7Main.this.statusLabel.getText());
                }
            }
        }.execute();
    }

    private void loadMoreResults() {
        if (!Page.isValid(this.nextPage)) {
            this.loadMoreButton.setVisible(false);
            return;
        }
        this.setBusy(true, "Cargando más...");
        this.loadMoreButton.setEnabled(false);
        new SwingWorker<ListExtractor.InfoItemsPage<InfoItem>, Void>() {
            @Override
            protected ListExtractor.InfoItemsPage<InfoItem> doInBackground() throws Exception {
                if (Win7Main.this.activeChannelExtractor != null) {
                    return Win7Main.this.activeChannelExtractor.getPage(Win7Main.this.nextPage);
                }
                if (Win7Main.this.activeSearchHandler == null) {
                    throw new IOException("No hay búsqueda activa para continuar.");
                }
                return SearchInfo.getMoreItems(YOUTUBE, Win7Main.this.activeSearchHandler, Win7Main.this.nextPage);
            }

            @Override
            protected void done() {
                try {
                    ListExtractor.InfoItemsPage<InfoItem> page = this.get();
                    Win7Main.this.appendStreams(page.getItems());
                    Win7Main.this.rememberChannels(Win7Main.streamItems(page.getItems(), Integer.MAX_VALUE));
                    Win7Main.this.nextPage = page.getNextPage();
                    Win7Main.this.statusLabel.setText(Win7Main.this.resultModel.size() + " videos cargados.");
                    Win7Main.this.updateLoadMoreButton();
                } catch (Exception exception) {
                    Win7Main.this.showFailure("Error al cargar más videos", exception);
                    Win7Main.this.loadMoreButton.setEnabled(Page.isValid(Win7Main.this.nextPage));
                } finally {
                    Win7Main.this.setBusy(false, Win7Main.this.statusLabel.getText());
                }
            }
        }.execute();
    }

    private void openSelectedChannel() {
        StreamInfoItem item = this.resultList.getSelectedValue();
        if (item == null || item.getUploaderUrl() == null || item.getUploaderUrl().trim().isEmpty()) {
            this.statusLabel.setText("Canal no disponible para este video.");
            return;
        }
        this.openChannel(item.getUploaderUrl(), item.getUploaderName());
    }

    private void openChannel(final String channelUrl, final String channelName) {
        this.rememberChannel(channelName, channelUrl);
        this.previousResults = new ArrayList<>();
        for (int i = 0; i < this.resultModel.size(); i++) {
            this.previousResults.add(this.resultModel.get(i));
        }
        this.previousNextPage = this.nextPage;
        this.previousSearchHandler = this.activeSearchHandler;
        this.previousChannelExtractor = this.activeChannelExtractor;
        this.previousChannelName = this.activeChannelName;
        this.previousRecommendationsMode = this.recommendationsMode;

        this.setBusy(true, "Abriendo canal...");
        this.loadMoreButton.setVisible(false);
        this.viewChannelButton.setVisible(false);
        this.playButton.setEnabled(false);

        new SwingWorker<ChannelResults, Void>() {
            @Override
            protected ChannelResults doInBackground() throws Exception {
                ChannelInfo info = ChannelInfo.getInfo(YOUTUBE, channelUrl);
                if (info.getTabs().isEmpty()) {
                    throw new IOException("El canal no ofrece lista de videos.");
                }
                ListLinkHandler tabHandler = (ListLinkHandler) info.getTabs().get(0);
                ChannelTabExtractor extractor = YOUTUBE.getChannelTabExtractor(tabHandler);
                extractor.fetchPage();
                ListExtractor.InfoItemsPage<InfoItem> page = extractor.getInitialPage();
                return new ChannelResults(
                    info.getName() == null || info.getName().trim().isEmpty() ? channelName : info.getName(),
                    info.getUrl(), extractor, page
                );
            }

            @Override
            protected void done() {
                try {
                    ChannelResults channel = this.get();
                    Win7Main.this.activeChannelExtractor = channel.extractor;
                    Win7Main.this.activeChannelName = channel.name;
                    Win7Main.this.activeSearchHandler = null;
                    Win7Main.this.nextPage = channel.page.getNextPage();
                    Win7Main.this.recommendationsMode = false;
                    Win7Main.this.resultModel.clear();
                    Win7Main.this.appendStreams(channel.page.getItems());
                    Win7Main.this.backToResultsButton.setVisible(true);
                    Win7Main.this.statusLabel.setText(channel.name + " · " + Win7Main.this.resultModel.size() + " videos");
                    Win7Main.this.sectionTitle.setText("CANAL: " + channel.name);
                    Win7Main.this.channelRecommendationsPanel.setVisible(false);
                    Win7Main.this.updateLoadMoreButton();
                } catch (Exception exception) {
                    Win7Main.this.showFailure("No se pudo abrir el canal", exception);
                    Win7Main.this.viewChannelButton.setVisible(true);
                } finally {
                    Win7Main.this.setBusy(false, Win7Main.this.statusLabel.getText());
                }
            }
        }.execute();
    }

    private void restorePreviousResults() {
        if (this.previousResults == null) return;
        this.resultModel.clear();
        for (StreamInfoItem item : this.previousResults) {
            this.resultModel.addElement(item);
        }
        this.nextPage = this.previousNextPage;
        this.activeSearchHandler = this.previousSearchHandler;
        this.activeChannelExtractor = this.previousChannelExtractor;
        this.activeChannelName = this.previousChannelName;
        this.recommendationsMode = this.previousRecommendationsMode;
        this.previousResults = null;
        this.backToResultsButton.setVisible(false);
        this.channelRecommendationsPanel.setVisible(this.recommendationsMode);
        this.resultList.clearSelection();
        this.videoDetailsLabel.setText("Selecciona un video");
        this.viewChannelButton.setVisible(false);
        this.playButton.setEnabled(false);
        this.statusLabel.setText(this.resultModel.size() + " resultados restaurados.");
        this.sectionTitle.setText(this.recommendationsMode ? "RECOMENDADOS PARA TI" : "RESULTADOS DE BÚSQUEDA");
        this.updateLoadMoreButton();
    }

    private void loadHomePage() {
        this.previousResults = null;
        this.backToResultsButton.setVisible(false);
        this.resultList.clearSelection();
        this.videoDetailsLabel.setText("Selecciona un video");
        this.viewChannelButton.setVisible(false);
        this.playButton.setEnabled(false);
        this.infoCache.clear();
        this.loadRecentRecommendations();
    }

    private void updateLoadMoreButton() {
        boolean canLoadMore = Page.isValid(this.nextPage);
        this.loadMoreButton.setVisible(canLoadMore);
        this.loadMoreButton.setEnabled(canLoadMore);
    }

    private void appendStreams(List<InfoItem> items) {
        for (StreamInfoItem item : streamItems(items, Integer.MAX_VALUE)) {
            this.resultModel.addElement(item);
        }
    }

    private static List<StreamInfoItem> streamItems(List<InfoItem> items, int limit) {
        List<StreamInfoItem> streams = new ArrayList<>();
        for (InfoItem item : items) {
            if (!(item instanceof StreamInfoItem) || item.getUrl() == null || item.getUrl().isEmpty()) continue;
            streams.add((StreamInfoItem) item);
            if (streams.size() == limit) break;
        }
        return streams;
    }

    private static void addUniqueStreams(List<StreamInfoItem> destination, Set<String> urls, List<InfoItem> source, int limit) {
        int added = 0;
        for (StreamInfoItem item : streamItems(source, Integer.MAX_VALUE)) {
            if (!urls.add(item.getUrl())) continue;
            destination.add(item);
            if (++added == limit || destination.size() >= MAX_RESULTS) return;
        }
    }

    private void rememberSearch(String query) {
        List<String> searches = this.getRecentSearches();
        searches.remove(query);
        searches.add(0, query);
        for (int i = 0; i < MAX_RECENT_SEARCHES; i++) {
            String key = RECENT_SEARCH_KEY_PREFIX + i;
            if (i < searches.size()) {
                this.preferences.put(key, searches.get(i));
            } else {
                this.preferences.remove(key);
            }
        }
        this.preferences.putInt(RECENT_SEARCH_COUNT_KEY, Math.min(MAX_RECENT_SEARCHES, searches.size()));
    }

    private List<String> getRecentSearches() {
        int count = Math.min(MAX_RECENT_SEARCHES, this.preferences.getInt(RECENT_SEARCH_COUNT_KEY, 0));
        List<String> searches = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String query = this.preferences.get(RECENT_SEARCH_KEY_PREFIX + i, "").trim();
            if (!query.isEmpty()) searches.add(query);
        }
        return searches;
    }

    private void rememberChannels(List<StreamInfoItem> items) {
        for (StreamInfoItem item : items) {
            this.rememberChannel(item.getUploaderName(), item.getUploaderUrl());
        }
    }

    private void rememberChannel(String name, String url) {
        if (url == null || url.trim().isEmpty()) return;
        String channelUrl = url.trim();
        List<RecentChannel> channels = this.getRecentChannels();
        channels.removeIf(c -> c.url.equals(channelUrl));
        channels.add(0, new RecentChannel(name == null || name.trim().isEmpty() ? "Canal de YouTube" : name.trim(), channelUrl));
        for (int i = 0; i < MAX_RECENT_CHANNELS; i++) {
            String key = RECENT_CHANNEL_KEY_PREFIX + i + ".";
            if (i < channels.size()) {
                this.preferences.put(key + "name", channels.get(i).name);
                this.preferences.put(key + "url", channels.get(i).url);
            } else {
                this.preferences.remove(key + "name");
                this.preferences.remove(key + "url");
            }
        }
        this.preferences.putInt(RECENT_CHANNEL_COUNT_KEY, Math.min(MAX_RECENT_CHANNELS, channels.size()));
    }

    private List<RecentChannel> getRecentChannels() {
        int count = Math.min(MAX_RECENT_CHANNELS, this.preferences.getInt(RECENT_CHANNEL_COUNT_KEY, 0));
        List<RecentChannel> channels = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String key = RECENT_CHANNEL_KEY_PREFIX + i + ".";
            String name = this.preferences.get(key + "name", "").trim();
            String url = this.preferences.get(key + "url", "").trim();
            if (!name.isEmpty() && !url.isEmpty()) {
                channels.add(new RecentChannel(name, url));
            }
        }
        return channels;
    }

    private void refreshChannelRecommendations() {
        this.channelButtonsPanel.removeAll();
        List<RecentChannel> channels = this.getRecentChannels();
        for (RecentChannel channel : channels) {
            String label = channel.name.length() > 20 ? channel.name.substring(0, 18) + "..." : channel.name;
            ModernButton btn = new ModernButton(label, false);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            btn.setToolTipText(channel.name);
            btn.addActionListener(e -> this.openChannel(channel.url, channel.name));
            this.channelButtonsPanel.add(btn);
        }
        this.channelRecommendationsPanel.setVisible(this.recommendationsMode && !channels.isEmpty());
        this.channelButtonsPanel.revalidate();
        this.channelButtonsPanel.repaint();
    }

    /**
     * Reproducción optimizada con instancia única de VLC y protección contra caídas de cuadros para Atom N455.
     */
    private void playSelected() {
        final StreamInfoItem item = this.resultList.getSelectedValue();
        if (item == null) {
            this.statusLabel.setText("Selecciona un video primero.");
            return;
        }
        this.setBusy(true, "Resolviendo stream...");
        new SwingWorker<PlaybackTarget, Void>() {
            @Override
            protected PlaybackTarget doInBackground() throws Exception {
                StreamInfo streamInfo = Win7Main.this.getStreamInfo(item.getUrl());
                boolean isAudioOnly = Win7Main.this.audioOnlyCheckBox.isSelected();

                if (isAudioOnly) {
                    AudioStream audio = streamInfo.getAudioStreams().stream()
                        .max(Comparator.comparingInt(AudioStream::getAverageBitrate))
                        .orElse(null);
                    if (audio != null) {
                        return new PlaybackTarget(audio.getContent(), "Audio " + audio.getAverageBitrate() + "kbps", streamInfo, true);
                    }
                }

                // Intentar stream MP4 progresivo con audio hasta la resolución seleccionada
                Optional<VideoStream> selected = streamInfo.getVideoStreams().stream()
                    .filter(Win7Main::isCompatibleProgressiveStream)
                    .filter(s -> Win7Main.heightOf(s) <= selectedMaxResolution)
                    .filter(s -> Win7Main.heightOf(s) > 0)
                    .max(Comparator.comparingInt(Win7Main::heightOf));

                if (selected.isPresent()) {
                    VideoStream video = selected.get();
                    return new PlaybackTarget(video.getContent(), video.getResolution(), streamInfo, false);
                }

                // Fallback 1: Cualquier stream progresivo disponible
                Optional<VideoStream> anyProgressive = streamInfo.getVideoStreams().stream()
                    .filter(Win7Main::isCompatibleProgressiveStream)
                    .min(Comparator.comparingInt(Win7Main::heightOf));

                if (anyProgressive.isPresent()) {
                    VideoStream video = anyProgressive.get();
                    return new PlaybackTarget(video.getContent(), video.getResolution(), streamInfo, false);
                }

                // Fallback 2: Stream de audio de alta fidelidad si no hay video progresivo
                AudioStream audioFallback = streamInfo.getAudioStreams().stream()
                    .max(Comparator.comparingInt(AudioStream::getAverageBitrate))
                    .orElse(null);
                if (audioFallback != null) {
                    return new PlaybackTarget(audioFallback.getContent(), "Audio Directo", streamInfo, true);
                }

                throw new IOException("No se encontró stream compatible de YouTube.");
            }

            @Override
            protected void done() {
                try {
                    PlaybackTarget target = this.get();
                    if (Win7Main.this.vlcExecutable == null && !Win7Main.this.chooseVlcExecutable()) {
                        Win7Main.this.statusLabel.setText("Falta indicar VLC x86.");
                        return;
                    }
                    if (!Files.isRegularFile(Win7Main.this.vlcExecutable)) {
                        Win7Main.this.vlcExecutable = null;
                        if (!Win7Main.this.chooseVlcExecutable()) {
                            Win7Main.this.statusLabel.setText("Falta indicar VLC x86.");
                            return;
                        }
                    }

                    if (Win7Main.this.activePlayerProcess != null && Win7Main.this.activePlayerProcess.isAlive()) {
                        Win7Main.this.activePlayerProcess.destroy();
                    }

                    List<String> cmd = new ArrayList<>();
                    cmd.add(Win7Main.this.vlcExecutable.toString());
                    cmd.add("--one-instance");
                    cmd.add("--no-video-title-show");
                    cmd.add("--network-caching=2500");
                    cmd.add("--drop-late-frames");
                    cmd.add("--skip-frames");

                    if (target.isAudioOnly) {
                        cmd.add("--no-video");
                    }

                    cmd.add(target.url);

                    Win7Main.this.activePlayerProcess = new ProcessBuilder(cmd).start();
                    Win7Main.this.statusLabel.setText("Reproduciendo en VLC (" + target.resolution + ")...");
                } catch (Exception exception) {
                    Win7Main.this.showFailure("No se pudo iniciar reproducción", exception);
                } finally {
                    Win7Main.this.setBusy(false, Win7Main.this.statusLabel.getText());
                }
            }
        }.execute();
    }

    private void showSelectedVideo() {
        final StreamInfoItem item = this.resultList.getSelectedValue();
        if (item == null) {
            this.videoDetailsLabel.setText("Selecciona un video");
            this.viewChannelButton.setVisible(false);
            this.playButton.setEnabled(false);
            return;
        }
        this.playButton.setEnabled(true);
        this.viewChannelButton.setVisible(item.getUploaderUrl() != null && !item.getUploaderUrl().trim().isEmpty());
        this.videoDetailsLabel.setText("Cargando detalles de " + (item.getUploaderName() == null ? "video" : item.getUploaderName()) + "...");

        new SwingWorker<StreamInfo, Void>() {
            @Override
            protected StreamInfo doInBackground() throws Exception {
                return Win7Main.this.getStreamInfo(item.getUrl());
            }

            @Override
            protected void done() {
                if (Win7Main.this.resultList.getSelectedValue() != item) return;
                try {
                    StreamInfo info = this.get();
                    Locale lang = info.getLanguageInfo();
                    String langStr = (lang == null || lang.getLanguage().isEmpty()) ? "No informado" : lang.getDisplayName(new Locale("es"));
                    int subsCount = info.getSubtitles().size();
                    Win7Main.this.videoDetailsLabel.setText(
                        "Canal: " + item.getUploaderName() + " | Idioma: " + langStr + " | Subtítulos: " + (subsCount > 0 ? (subsCount + " pistas") : "Ninguno")
                    );
                } catch (Exception exception) {
                    Win7Main.this.videoDetailsLabel.setText("Canal: " + (item.getUploaderName() == null ? "YouTube" : item.getUploaderName()));
                }
            }
        }.execute();
    }

    private StreamInfo getStreamInfo(String url) throws Exception {
        StreamInfo cached = this.infoCache.get(url);
        if (cached != null) return cached;
        StreamInfo loaded = StreamInfo.getInfo(YOUTUBE, url);
        this.infoCache.put(url, loaded);
        return loaded;
    }

    private static boolean isCompatibleProgressiveStream(VideoStream stream) {
        return !stream.isVideoOnly() && stream.isUrl() && stream.getFormat() == MediaFormat.MPEG_4 && stream.getDeliveryMethod() == DeliveryMethod.PROGRESSIVE_HTTP;
    }

    private static int heightOf(VideoStream stream) {
        Matcher matcher = HEIGHT_PATTERN.matcher(stream.getResolution());
        int height = 0;
        while (matcher.find()) {
            height = Integer.parseInt(matcher.group(1));
        }
        return height;
    }

    private boolean chooseVlcExecutable() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecciona VLC de 32 bits (vlc.exe)");
        chooser.setFileFilter(new FileNameExtensionFilter("Ejecutable VLC (vlc.exe)", "exe"));
        if (this.vlcExecutable != null && Files.isRegularFile(this.vlcExecutable)) {
            chooser.setSelectedFile(this.vlcExecutable.toFile());
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        Path selected = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        if (!Files.isRegularFile(selected) || !selected.getFileName().toString().equalsIgnoreCase("vlc.exe")) {
            JOptionPane.showMessageDialog(this, "Por favor selecciona un archivo vlc.exe válido de 32 bits.", "Archivo incorrecto", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        this.vlcExecutable = selected;
        this.preferences.put(VLC_PREFERENCE_KEY, selected.toString());
        return true;
    }

    private Path loadVlcExecutable() {
        String savedPath = this.preferences.get(VLC_PREFERENCE_KEY, "");
        if (!savedPath.isEmpty()) {
            Path p = Paths.get(savedPath);
            if (Files.isRegularFile(p)) return p;
        }
        String[] common = new String[]{
            "C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc.exe",
            "C:\\Program Files\\VideoLAN\\VLC\\vlc.exe",
            "E:\\portable\\vlc\\vlc.exe",
            "D:\\portable\\vlc\\vlc.exe",
            "C:\\VLC\\vlc.exe"
        };
        for (String c : common) {
            Path p = Paths.get(c);
            if (Files.isRegularFile(p)) return p;
        }
        return null;
    }

    private void setBusy(boolean busy, String status) {
        this.searchButton.setEnabled(!busy);
        this.searchField.setEnabled(!busy);
        this.resultList.setEnabled(!busy);
        this.viewChannelButton.setEnabled(!busy);
        this.playButton.setEnabled(!busy && this.resultList.getSelectedValue() != null);
        this.backToResultsButton.setEnabled(!busy);
        this.homeButton.setEnabled(!busy);
        if (busy) this.loadMoreButton.setEnabled(false);
        this.statusLabel.setText(status);
    }

    private void showFailure(String message, Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String detail = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        this.statusLabel.setText(message + ": " + detail);

        // Guardar detalle en archivo para soporte y auditoría
        try (PrintWriter pw = new PrintWriter(new FileWriter("error_debug.log", true))) {
            pw.println("=== " + new Date() + " : " + message + " ===");
            cause.printStackTrace(pw);
            pw.println();
        } catch (Exception ignored) {}

        JOptionPane.showMessageDialog(this, message + "\n\nDetalle: " + detail, "Aviso del Sistema", JOptionPane.WARNING_MESSAGE);
    }

    private static String formatDuration(long seconds) {
        if (seconds < 0L) return "--:--";
        long h = seconds / 3600L;
        long m = (seconds % 3600L) / 60L;
        long s = seconds % 60L;
        if (h > 0L) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s);
        }
        return String.format(Locale.ROOT, "%02d:%02d", m, s);
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private List<String> thumbnailUrls(StreamInfoItem item) {
        List<String> urls = new ArrayList<>();
        String videoId = youtubeVideoId(item.getUrl());
        if (videoId != null) {
            urls.add("https://i.ytimg.com/vi/" + videoId + "/mqdefault.jpg"); // 320x180 JPEG rápido
            urls.add("https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg"); // 480x360 JPEG
        }
        for (Image img : item.getThumbnails()) {
            if (img != null && img.getUrl() != null && !urls.contains(img.getUrl())) {
                urls.add(img.getUrl());
            }
        }
        return urls;
    }

    private static String youtubeVideoId(String videoUrl) {
        if (videoUrl == null) return null;
        Matcher m = YOUTUBE_VIDEO_ID_PATTERN.matcher(videoUrl);
        return m.find() ? m.group(1) : null;
    }

    private BufferedImage getCachedThumbnail(String url) {
        synchronized (this.thumbnailCache) {
            return this.thumbnailCache.get(url);
        }
    }

    private void requestThumbnail(String url, List<String> alternatives) {
        if (url == null || getCachedThumbnail(url) != null || failedThumbnailUrls.contains(url) || !thumbnailLoads.add(url)) {
            return;
        }
        try {
            this.thumbnailExecutor.execute(() -> {
                try {
                    for (String candidate : alternatives) {
                        try {
                            BufferedImage img = downloadThumbnail(candidate);
                            if (img != null) {
                                synchronized (this.thumbnailCache) {
                                    this.thumbnailCache.put(url, img);
                                }
                                SwingUtilities.invokeLater(this.resultList::repaint);
                                return;
                            }
                        } catch (IOException ignored) {}
                    }
                    this.failedThumbnailUrls.add(url);
                } finally {
                    this.thumbnailLoads.remove(url);
                }
            });
        } catch (RejectedExecutionException e) {
            this.thumbnailLoads.remove(url);
            this.failedThumbnailUrls.add(url);
        }
    }

    private static BufferedImage downloadThumbnail(String imageUrl) throws IOException {
        URI uri;
        try {
            uri = URI.create(imageUrl);
        } catch (IllegalArgumentException e) {
            throw new IOException("URL inválida", e);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IOException("Solo se permite HTTPS");
        }
        HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
        if (conn instanceof javax.net.ssl.HttpsURLConnection) {
            javax.net.ssl.HttpsURLConnection https = (javax.net.ssl.HttpsURLConnection) conn;
            https.setSSLSocketFactory(SslHelper.getSocketFactory());
            https.setHostnameVerifier(SslHelper.getHostnameVerifier());
        }
        try {
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(8000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0");
            conn.setRequestProperty("Accept", "image/jpeg,image/png,*/*;q=0.5");
            if (conn.getResponseCode() != 200) {
                throw new IOException("HTTP " + conn.getResponseCode());
            }
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int total = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_THUMBNAIL_BYTES) throw new IOException("Tamaño excedido");
                    out.write(buffer, 0, read);
                }
                return decodeThumbnail(out.toByteArray());
            }
        } finally {
            conn.disconnect();
        }
    }

    private static BufferedImage decodeThumbnail(byte[] data) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (input == null) throw new IOException("Sin stream de imagen");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Formato no compatible");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || w > 3000 || h > 3000) throw new IOException("Dimensiones inválidas");

                int sample = Math.max(1, Math.min(w / THUMBNAIL_WIDTH, h / THUMBNAIL_HEIGHT));
                ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceSubsampling(sample, sample, 0, 0);
                BufferedImage decoded = reader.read(0, param);
                if (decoded == null) throw new IOException("Fallo de decodificación");

                BufferedImage thumb = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = thumb.createGraphics();
                g.setColor(HEADER_COLOR);
                g.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
                double scale = Math.min((double) THUMBNAIL_WIDTH / decoded.getWidth(), (double) THUMBNAIL_HEIGHT / decoded.getHeight());
                int sw = Math.max(1, (int) (decoded.getWidth() * scale));
                int sh = Math.max(1, (int) (decoded.getHeight() * scale));
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(decoded, (THUMBNAIL_WIDTH - sw) / 2, (THUMBNAIL_HEIGHT - sh) / 2, sw, sh, null);
                g.dispose();
                return thumb;
            } finally {
                reader.dispose();
            }
        }
    }

    private static BufferedImage createThumbnailPlaceholder() {
        BufferedImage placeholder = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = placeholder.createGraphics();
        g.setColor(new Color(25, 33, 44));
        g.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        g.setColor(new Color(60, 75, 95));
        g.fillRoundRect(THUMBNAIL_WIDTH / 2 - 14, THUMBNAIL_HEIGHT / 2 - 11, 28, 22, 6, 6);
        g.setColor(Color.WHITE);
        g.fillPolygon(
            new int[]{THUMBNAIL_WIDTH / 2 - 4, THUMBNAIL_WIDTH / 2 - 4, THUMBNAIL_WIDTH / 2 + 6},
            new int[]{THUMBNAIL_HEIGHT / 2 - 6, THUMBNAIL_HEIGHT / 2 + 6, THUMBNAIL_HEIGHT / 2},
            3
        );
        g.dispose();
        return placeholder;
    }

    // --- Componentes Gráficos Personalizados ---

    private static final class ModernButton extends JButton {
        private final boolean primary;

        private ModernButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            this.setContentAreaFilled(false);
            this.setBorderPainted(false);
            this.setFocusPainted(false);
            this.setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = this.primary ? ACCENT_COLOR : CARD_COLOR;
            if (!this.isEnabled()) {
                fill = this.primary ? new Color(180, 100, 100) : new Color(60, 60, 60);
            } else if (this.getModel().isPressed()) {
                fill = this.primary ? new Color(160, 35, 35) : new Color(80, 80, 80);
            } else if (this.getModel().isRollover()) {
                fill = this.primary ? new Color(220, 50, 50) : ACCENT_LIGHT_COLOR;
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 8, 8);
            if (!this.primary) {
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, 8, 8);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class ThumbnailThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "newpipe-thumb-loader");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    }

    public enum Theme {
        DARK("dark", "Oscuro (Canaima)", new Color(15, 23, 42), new Color(17, 24, 39), new Color(31, 41, 55), new Color(243, 246, 250), new Color(156, 163, 175), new Color(55, 65, 81), new Color(239, 68, 68), new Color(67, 39, 48)),
        LIGHT("light", "Claro", new Color(24, 34, 48), new Color(244, 246, 250), Color.WHITE, new Color(31, 41, 55), new Color(107, 119, 139), new Color(226, 231, 238), new Color(220, 38, 38), new Color(254, 226, 226)),
        OCEAN("ocean", "Océano", new Color(12, 45, 67), new Color(235, 246, 250), Color.WHITE, new Color(20, 53, 70), new Color(84, 117, 132), new Color(205, 226, 233), new Color(14, 116, 144), new Color(207, 250, 254)),
        FOREST("forest", "Verde", new Color(29, 51, 43), new Color(240, 247, 242), Color.WHITE, new Color(34, 57, 45), new Color(94, 119, 101), new Color(213, 229, 216), new Color(21, 128, 61), new Color(220, 252, 231));

        public final String id;
        public final String label;
        public final Color header;
        public final Color page;
        public final Color card;
        public final Color text;
        public final Color muted;
        public final Color border;
        public final Color accent;
        public final Color accentLight;

        Theme(String id, String label, Color header, Color page, Color card, Color text, Color muted, Color border, Color accent, Color accentLight) {
            this.id = id;
            this.label = label;
            this.header = header;
            this.page = page;
            this.card = card;
            this.text = text;
            this.muted = muted;
            this.border = border;
            this.accent = accent;
            this.accentLight = accentLight;
        }

        public static Theme fromId(String id) {
            for (Theme t : values()) {
                if (t.id.equals(id)) return t;
            }
            return DARK;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    private static final class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        private RoundedBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.color);
            g2.drawRoundRect(x, y, width - 1, height - 1, this.radius, this.radius);
            g2.dispose();
        }
    }

    private final class ResultCellRenderer extends JPanel implements ListCellRenderer<StreamInfoItem> {
        private final JLabel thumbLabel = new JLabel();
        private final JLabel textLabel = new JLabel();

        private ResultCellRenderer() {
            super(new BorderLayout(10, 0));
            this.setOpaque(true);
            this.thumbLabel.setPreferredSize(new Dimension(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));
            this.thumbLabel.setHorizontalAlignment(JLabel.CENTER);
            this.textLabel.setOpaque(false);
            this.add(this.thumbLabel, BorderLayout.WEST);
            this.add(this.textLabel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends StreamInfoItem> list, StreamInfoItem item, int index, boolean isSelected, boolean cellHasFocus) {
            String uploader = item.getUploaderName() == null ? "Canal desconocido" : item.getUploaderName();
            String duration = Win7Main.formatDuration(item.getDuration());

            this.textLabel.setText(
                "<html><body style='padding: 2px 0;'>" +
                "<div style='font-size: 11px; font-weight: bold;'>" + Win7Main.escapeHtml(item.getName()) + "</div>" +
                "<div style='font-size: 10px; color: #9CA3AF; margin-top: 2px;'>" +
                Win7Main.escapeHtml(uploader) + "  ·  " + duration + "</div>" +
                "</body></html>"
            );

            List<String> imageUrls = Win7Main.this.thumbnailUrls(item);
            String url = imageUrls.isEmpty() ? null : imageUrls.get(0);
            BufferedImage img = url == null ? null : Win7Main.this.getCachedThumbnail(url);

            if (img != null) {
                BufferedImage decorated = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = decorated.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.setFont(new Font("Segoe UI", Font.BOLD, 9));
                int txtW = g.getFontMetrics().stringWidth(duration);
                g.setColor(new Color(0, 0, 0, 180));
                g.fillRoundRect(THUMBNAIL_WIDTH - txtW - 8, THUMBNAIL_HEIGHT - 16, txtW + 6, 13, 4, 4);
                g.setColor(Color.WHITE);
                g.drawString(duration, THUMBNAIL_WIDTH - txtW - 5, THUMBNAIL_HEIGHT - 6);
                g.dispose();
                this.thumbLabel.setIcon(new ImageIcon(decorated));
            } else {
                this.thumbLabel.setIcon(new ImageIcon(thumbnailPlaceholder));
                Win7Main.this.requestThumbnail(url, imageUrls);
            }

            this.setBackground(isSelected ? ACCENT_LIGHT_COLOR : CARD_COLOR);
            this.textLabel.setForeground(TEXT_COLOR);
            Color marker = isSelected ? ACCENT_COLOR : CARD_COLOR;
            this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 1, 0, marker),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
            ));
            return this;
        }
    }

    private static final class RecentChannel {
        final String name;
        final String url;

        RecentChannel(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    private static final class ChannelResults {
        final String name;
        final String url;
        final ChannelTabExtractor extractor;
        final ListExtractor.InfoItemsPage<InfoItem> page;

        ChannelResults(String name, String url, ChannelTabExtractor extractor, ListExtractor.InfoItemsPage<InfoItem> page) {
            this.name = name;
            this.url = url;
            this.extractor = extractor;
            this.page = page;
        }
    }

    private static final class RecommendationResults {
        final List<StreamInfoItem> items;
        final SearchQueryHandler queryHandler;
        final Page nextPage;

        RecommendationResults(List<StreamInfoItem> items, SearchQueryHandler queryHandler, Page nextPage) {
            this.items = items;
            this.queryHandler = queryHandler;
            this.nextPage = nextPage;
        }
    }

    private static final class PlaybackTarget {
        final String url;
        final String resolution;
        final StreamInfo streamInfo;
        final boolean isAudioOnly;

        PlaybackTarget(String url, String resolution, StreamInfo streamInfo, boolean isAudioOnly) {
            this.url = url;
            this.resolution = resolution;
            this.streamInfo = streamInfo;
            this.isAudioOnly = isAudioOnly;
        }
    }
}
