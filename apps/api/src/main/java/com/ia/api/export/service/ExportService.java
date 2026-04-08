package com.ia.api.export.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.export.api.ExportAuditItem;
import com.ia.api.export.api.ExportHistoryResponse;
import com.ia.api.export.api.ExportRequest;
import com.ia.api.export.domain.ExportAuditEntity;
import com.ia.api.export.repository.ExportAuditRepository;
import com.ia.api.goal.domain.GoalEntity;
import com.ia.api.goal.repository.GoalRepository;
import com.ia.api.task.domain.TaskDefinitionEntity;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.repository.TaskDefinitionRepository;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    // Palette couleurs Excel
    private static final short COL_HEADER_BG  = IndexedColors.DARK_TEAL.getIndex();
    private static final short COL_ALT_ROW_BG = IndexedColors.LIGHT_TURQUOISE.getIndex();
    private static final short COL_TITLE_BG   = IndexedColors.DARK_BLUE.getIndex();

    // Palette couleurs PDF
    private static final Color PDF_HEADER_BG   = new Color(0, 102, 102);
    private static final Color PDF_HEADER_TEXT = Color.WHITE;
    private static final Color PDF_ROW_ALT     = new Color(230, 245, 245);
    private static final Color PDF_TITLE_COLOR = new Color(0, 51, 102);

    private final UserRepository userRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final GoalRepository goalRepository;
    private final ExportAuditRepository exportAuditRepository;

    public ExportService(
            UserRepository userRepository,
            TaskDefinitionRepository taskDefinitionRepository,
            TaskOccurrenceRepository taskOccurrenceRepository,
            GoalRepository goalRepository,
            ExportAuditRepository exportAuditRepository
    ) {
        this.userRepository = userRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.taskOccurrenceRepository = taskOccurrenceRepository;
        this.goalRepository = goalRepository;
        this.exportAuditRepository = exportAuditRepository;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public ExportHistoryResponse listHistory(String email) {
        UserEntity user = getUser(email);
        List<ExportAuditItem> items = exportAuditRepository
                .findTop10ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toItem)
                .toList();
        return new ExportHistoryResponse(items);
    }

    public ExportPayload generateExport(String email, ExportRequest request) {
        UserEntity user = getUser(email);
        ExportParameters params = normalize(user, request);

        ExportAuditEntity audit = new ExportAuditEntity();
        audit.setUserId(user.getId());
        audit.setExportFormat(params.format());
        audit.setExportScope(params.scope());
        audit.setPeriodFrom(params.fromDate());
        audit.setPeriodTo(params.toDate());
        audit.setStatus("STARTED");
        exportAuditRepository.save(audit);

        long startedAt = System.nanoTime();
        try {
            ExportDataset dataset = loadDataset(user.getId(), params);
            byte[] content = switch (params.format()) {
                case "EXCEL" -> buildExcel(user, dataset, params);
                case "PDF"   -> buildPdf(user, dataset, params);
                default      -> throw new IllegalArgumentException("Format d'export non supporté.");
            };
            String contentType = params.format().equals("EXCEL") ? EXCEL_CONTENT_TYPE : PDF_CONTENT_TYPE;
            String fileName = buildFileName(params);

            audit.setStatus("SUCCESS");
            audit.setFileName(fileName);
            audit.setRowCount(dataset.totalRows());
            audit.setDurationMs((System.nanoTime() - startedAt) / 1_000_000);
            audit.setCompletedAt(Instant.now());
            exportAuditRepository.save(audit);

            return new ExportPayload(fileName, contentType, content);
        } catch (RuntimeException exception) {
            audit.setStatus("FAILED");
            audit.setErrorMessage(exception.getMessage());
            audit.setCompletedAt(Instant.now());
            audit.setDurationMs((System.nanoTime() - startedAt) / 1_000_000);
            exportAuditRepository.save(audit);
            throw exception;
        }
    }

    // -------------------------------------------------------------------------
    // Excel generation (Apache POI)
    // -------------------------------------------------------------------------

    private byte[] buildExcel(UserEntity user, ExportDataset dataset, ExportParameters params) {
        System.setProperty("java.awt.headless", "true");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            ExcelStyles styles = new ExcelStyles(wb);
            createSummarySheet(wb, styles, user, dataset, params);
            if (!dataset.taskDefinitions().isEmpty()) {
                createTasksSheet(wb, styles, dataset.taskDefinitions());
            }
            if (!dataset.occurrences().isEmpty()) {
                createHistorySheet(wb, styles, dataset.occurrences(), dataset.taskDefinitions());
            }
            if (!dataset.goals().isEmpty()) {
                createGoalsSheet(wb, styles, dataset.goals(), dataset.taskDefinitions());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de générer le fichier Excel.");
        }
    }

    private void createSummarySheet(XSSFWorkbook wb, ExcelStyles s,
                                    UserEntity user, ExportDataset dataset, ExportParameters params) {
        XSSFSheet sheet = wb.createSheet("Résumé");
        sheet.setColumnWidth(0, 7000);
        sheet.setColumnWidth(1, 7000);

        // Title
        XSSFRow titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(30);
        XSSFCell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Personal Habit Tracker — Export");
        titleCell.setCellStyle(s.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // Metadata
        addSummaryPair(sheet, s, 2, "Utilisateur", user.getEmail());
        addSummaryPair(sheet, s, 3, "Scope",  params.scope());
        addSummaryPair(sheet, s, 4, "Période", params.fromDate() + " → " + params.toDate());
        addSummaryPair(sheet, s, 5, "Généré le", LocalDate.now(ZoneId.of("Europe/Paris")).toString());

        // Stats
        long done = dataset.occurrences().stream().filter(o -> "done".equals(o.getStatus())).count();
        long missed = dataset.occurrences().stream().filter(o -> "missed".equals(o.getStatus())).count();
        long suspended = dataset.occurrences().stream().filter(o -> "suspended".equals(o.getStatus())).count();
        long total = dataset.occurrences().size();
        String rate = total > 0 ? String.format("%.0f%%", 100.0 * done / total) : "—";

        addSummaryPair(sheet, s, 7, "Tâches définies", String.valueOf(dataset.taskDefinitions().size()));
        addSummaryPair(sheet, s, 8, "Occurrences totales", String.valueOf(total));
        addSummaryPair(sheet, s, 9, "✅ Réalisées", String.valueOf(done));
        addSummaryPair(sheet, s, 10, "❌ Manquées", String.valueOf(missed));
        addSummaryPair(sheet, s, 11, "⏸ Suspendues", String.valueOf(suspended));
        addSummaryPair(sheet, s, 12, "Taux de complétion", rate);
        addSummaryPair(sheet, s, 13, "Objectifs définis", String.valueOf(dataset.goals().size()));
    }

    private void addSummaryPair(XSSFSheet sheet, ExcelStyles s, int rowIdx, String label, String value) {
        XSSFRow row = sheet.createRow(rowIdx);
        row.setHeightInPoints(18);
        XSSFCell lbl = row.createCell(0);
        lbl.setCellValue(label);
        lbl.setCellStyle(s.summaryLabel);
        XSSFCell val = row.createCell(1);
        val.setCellValue(value);
        val.setCellStyle(s.summaryValue);
    }

    private void createTasksSheet(XSSFWorkbook wb, ExcelStyles s, List<TaskDefinitionEntity> definitions) {
        XSSFSheet sheet = wb.createSheet("Tâches");
        int[] colWidths = {3000, 9000, 6000, 5000, 6000};
        for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i]);
        sheet.createFreezePane(0, 1);

        String[] headers = {"Type", "Titre", "Description", "Date création", "Dernière MAJ"};
        writeHeaderRow(sheet, s, headers);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));

        int rowIdx = 1;
        for (TaskDefinitionEntity def : definitions) {
            XSSFRow row = sheet.createRow(rowIdx);
            row.setHeightInPoints(16);
            XSSFCellStyle style = (rowIdx % 2 == 0) ? s.dataAlt : s.data;
            setCellStr(row, 0, taskTypeLabel(def.getTaskType()), style);
            setCellStr(row, 1, def.getTitle(), style);
            setCellStr(row, 2, def.getDescription() != null ? def.getDescription() : "", style);
            setCellStr(row, 3, def.getCreatedAt() != null ? formatInstant(def.getCreatedAt()) : "", style);
            setCellStr(row, 4, def.getUpdatedAt() != null ? formatInstant(def.getUpdatedAt()) : "", style);
            rowIdx++;
        }
    }

    private void createHistorySheet(XSSFWorkbook wb, ExcelStyles s,
                                    List<TaskOccurrenceEntity> occurrences,
                                    List<TaskDefinitionEntity> definitions) {
        Map<UUID, String> titleById = definitions.stream()
                .collect(Collectors.toMap(TaskDefinitionEntity::getId, TaskDefinitionEntity::getTitle));

        XSSFSheet sheet = wb.createSheet("Historique");
        int[] colWidths = {3500, 8000, 3000, 5000, 5000};
        for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i]);
        sheet.createFreezePane(0, 1);

        String[] headers = {"Date", "Tâche", "Heure", "Statut", "Catégorie"};
        writeHeaderRow(sheet, s, headers);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));

        int rowIdx = 1;
        for (TaskOccurrenceEntity occ : occurrences) {
            XSSFRow row = sheet.createRow(rowIdx);
            row.setHeightInPoints(16);
            XSSFCellStyle style = (rowIdx % 2 == 0) ? s.dataAlt : s.data;
            setCellStr(row, 0, occ.getOccurrenceDate().toString(), style);
            setCellStr(row, 1, titleById.getOrDefault(occ.getTaskDefinitionId(), occ.getTaskDefinitionId().toString()), style);
            setCellStr(row, 2, occ.getOccurrenceTime() != null ? occ.getOccurrenceTime().toString() : "", style);
            setCellStr(row, 3, statusLabel(occ.getStatus()), style);
            setCellStr(row, 4, dayCategoryLabel(occ.getDayCategory()), style);
            rowIdx++;
        }
    }

    private void createGoalsSheet(XSSFWorkbook wb, ExcelStyles s,
                                  List<GoalEntity> goals,
                                  List<TaskDefinitionEntity> definitions) {
        Map<UUID, String> titleById = definitions.stream()
                .collect(Collectors.toMap(TaskDefinitionEntity::getId, TaskDefinitionEntity::getTitle));

        XSSFSheet sheet = wb.createSheet("Objectifs");
        int[] colWidths = {4000, 4000, 3000, 3000, 7000};
        for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i]);
        sheet.createFreezePane(0, 1);

        String[] headers = {"Scope", "Période", "Cible", "Actif", "Tâche associée"};
        writeHeaderRow(sheet, s, headers);

        int rowIdx = 1;
        for (GoalEntity goal : goals) {
            XSSFRow row = sheet.createRow(rowIdx);
            row.setHeightInPoints(16);
            XSSFCellStyle style = (rowIdx % 2 == 0) ? s.dataAlt : s.data;
            setCellStr(row, 0, goalScopeLabel(goal.getGoalScope()), style);
            setCellStr(row, 1, periodTypeLabel(goal.getPeriodType()), style);
            setCellStr(row, 2, String.valueOf(goal.getTargetCount()), style);
            setCellStr(row, 3, goal.isActive() ? "Oui" : "Non", style);
            setCellStr(row, 4, goal.getTaskDefinitionId() != null
                    ? titleById.getOrDefault(goal.getTaskDefinitionId(), "—")
                    : "Toutes les tâches", style);
            rowIdx++;
        }
    }

    private void writeHeaderRow(XSSFSheet sheet, ExcelStyles s, String[] headers) {
        XSSFRow headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(22);
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(s.header);
        }
    }

    private void setCellStr(XSSFRow row, int col, String value, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    // -------------------------------------------------------------------------
    // PDF generation (openpdf + JFreeChart)
    // -------------------------------------------------------------------------

    private byte[] buildPdf(UserEntity user, ExportDataset dataset, ExportParameters params) {
        System.setProperty("java.awt.headless", "true");
        Map<UUID, String> titleById = dataset.taskDefinitions().stream()
                .collect(Collectors.toMap(TaskDefinitionEntity::getId, TaskDefinitionEntity::getTitle));
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addPdfHeader(doc, user, params);

            // Charts (if history data available)
            if (!dataset.occurrences().isEmpty()) {
                addPdfCharts(doc, dataset);
            }

            // Tables
            if (!dataset.taskDefinitions().isEmpty()) {
                addPdfSection(doc, "📋 Tâches définies");
                addPdfTasksTable(doc, dataset.taskDefinitions());
            }
            if (!dataset.occurrences().isEmpty()) {
                addPdfSection(doc, "📅 Historique des occurrences");
                addPdfHistoryTable(doc, dataset.occurrences(), titleById);
            }
            if (!dataset.goals().isEmpty()) {
                addPdfSection(doc, "🎯 Objectifs");
                addPdfGoalsTable(doc, dataset.goals(), titleById);
            }

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Impossible de générer le PDF.");
        }
    }

    private void addPdfHeader(Document doc, UserEntity user, ExportParameters params) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PDF_TITLE_COLOR);
        Font metaFont  = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

        Paragraph title = new Paragraph("Personal Habit Tracker — Export", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        Paragraph meta = new Paragraph(
                "Utilisateur : " + user.getEmail()
                + "   |   Scope : " + params.scope()
                + "   |   Période : " + params.fromDate() + " → " + params.toDate(),
                metaFont
        );
        meta.setAlignment(Element.ALIGN_CENTER);
        meta.setSpacingAfter(16);
        doc.add(meta);
    }

    private void addPdfCharts(Document doc, ExportDataset dataset) throws DocumentException, IOException {
        // Pie chart : distribution des statuts
        Map<String, Long> statusCounts = dataset.occurrences().stream()
                .collect(Collectors.groupingBy(TaskOccurrenceEntity::getStatus, Collectors.counting()));

        DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();
        statusCounts.forEach((status, count) -> pieDataset.setValue(statusLabel(status), count));

        JFreeChart pieChart = ChartFactory.createPieChart(
                "Distribution des statuts", pieDataset, true, false, false);
        styleChart(pieChart);
        PiePlot<?> piePlot = (PiePlot<?>) pieChart.getPlot();
        piePlot.setBackgroundPaint(Color.WHITE);
        piePlot.setOutlinePaint(null);

        // Bar chart : taux de complétion par jour (top 14 derniers jours)
        Map<LocalDate, long[]> byDay = new LinkedHashMap<>();
        dataset.occurrences().stream()
                .sorted((a, b) -> a.getOccurrenceDate().compareTo(b.getOccurrenceDate()))
                .forEach(occ -> {
                    long[] counts = byDay.computeIfAbsent(occ.getOccurrenceDate(), d -> new long[2]);
                    if ("done".equals(occ.getStatus())) counts[0]++;
                    counts[1]++;
                });

        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        // Limit to last 14 days with data
        List<LocalDate> days = byDay.keySet().stream().sorted().toList();
        List<LocalDate> last14 = days.size() > 14 ? days.subList(days.size() - 14, days.size()) : days;
        for (LocalDate day : last14) {
            long[] c = byDay.get(day);
            double rate = c[1] > 0 ? 100.0 * c[0] / c[1] : 0;
            barDataset.addValue(rate, "Taux de complétion", day.toString().substring(5)); // MM-DD
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Taux de complétion par jour (%)", null, "%",
                barDataset, PlotOrientation.VERTICAL, false, false, false);
        styleChart(barChart);
        CategoryPlot categoryPlot = barChart.getCategoryPlot();
        categoryPlot.setBackgroundPaint(Color.WHITE);
        categoryPlot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        categoryPlot.getRenderer().setSeriesPaint(0, new Color(0, 153, 153));

        // Render to PNG and embed side by side
        PdfPTable chartTable = new PdfPTable(2);
        chartTable.setWidthPercentage(100);
        chartTable.setWidths(new float[]{1, 1});
        chartTable.setSpacingBefore(8);
        chartTable.setSpacingAfter(16);

        chartTable.addCell(chartCell(pieChart, 300, 220));
        chartTable.addCell(chartCell(barChart, 360, 220));
        doc.add(chartTable);
    }

    private PdfPCell chartCell(JFreeChart chart, int width, int height) throws IOException {
        ByteArrayOutputStream chartOut = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(chartOut, chart, width, height);
        com.lowagie.text.Image img;
        try {
            img = com.lowagie.text.Image.getInstance(chartOut.toByteArray());
        } catch (DocumentException e) {
            throw new IOException("Impossible de créer l'image du graphique.", e);
        }
        img.setAlignment(Element.ALIGN_CENTER);
        PdfPCell cell = new PdfPCell(img, true);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private void addPdfSection(Document doc, String title) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, PDF_TITLE_COLOR);
        Paragraph section = new Paragraph(title, sectionFont);
        section.setSpacingBefore(14);
        section.setSpacingAfter(6);
        doc.add(section);
    }

    private void addPdfTasksTable(Document doc, List<TaskDefinitionEntity> definitions) throws DocumentException {
        String[] headers = {"Type", "Titre", "Description"};
        float[] widths = {2f, 4f, 6f};
        PdfPTable table = pdfTable(headers, widths);
        for (TaskDefinitionEntity def : definitions) {
            addPdfDataRow(table, taskTypeLabel(def.getTaskType()), def.getTitle(),
                    def.getDescription() != null ? def.getDescription() : "");
        }
        doc.add(table);
    }

    private void addPdfHistoryTable(Document doc, List<TaskOccurrenceEntity> occurrences,
                                    Map<UUID, String> titleById) throws DocumentException {
        String[] headers = {"Date", "Tâche", "Heure", "Statut", "Catégorie"};
        float[] widths = {2f, 5f, 2f, 3f, 3f};
        PdfPTable table = pdfTable(headers, widths);
        int limit = Math.min(occurrences.size(), 200);
        for (int i = 0; i < limit; i++) {
            TaskOccurrenceEntity occ = occurrences.get(i);
            addPdfDataRow(table,
                    occ.getOccurrenceDate().toString(),
                    titleById.getOrDefault(occ.getTaskDefinitionId(), "—"),
                    occ.getOccurrenceTime() != null ? occ.getOccurrenceTime().toString() : "",
                    statusLabel(occ.getStatus()),
                    dayCategoryLabel(occ.getDayCategory())
            );
        }
        if (occurrences.size() > 200) {
            Font noteFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
            doc.add(new Paragraph("… " + (occurrences.size() - 200) + " occurrences supplémentaires (utilisez l'export Excel pour le jeu complet).", noteFont));
        }
        doc.add(table);
    }

    private void addPdfGoalsTable(Document doc, List<GoalEntity> goals,
                                  Map<UUID, String> titleById) throws DocumentException {
        String[] headers = {"Scope", "Période", "Cible", "Actif", "Tâche associée"};
        float[] widths = {2f, 2f, 1.5f, 1.5f, 5f};
        PdfPTable table = pdfTable(headers, widths);
        for (GoalEntity goal : goals) {
            addPdfDataRow(table,
                    goalScopeLabel(goal.getGoalScope()),
                    periodTypeLabel(goal.getPeriodType()),
                    String.valueOf(goal.getTargetCount()),
                    goal.isActive() ? "Oui" : "Non",
                    goal.getTaskDefinitionId() != null
                            ? titleById.getOrDefault(goal.getTaskDefinitionId(), "—")
                            : "Toutes les tâches"
            );
        }
        doc.add(table);
    }

    private PdfPTable pdfTable(String[] headers, float[] widths) throws DocumentException {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        table.setSpacingAfter(12);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PDF_HEADER_TEXT);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(PDF_HEADER_BG);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
        table.setHeaderRows(1);
        return table;
    }

    private void addPdfDataRow(PdfPTable table, String... values) {
        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        boolean alt = (table.size() / table.getNumberOfColumns()) % 2 == 0;
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", dataFont));
            cell.setPadding(4);
            if (alt) cell.setBackgroundColor(PDF_ROW_ALT);
            table.addCell(cell);
        }
    }

    private void styleChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        chart.getTitle().setPaint(PDF_TITLE_COLOR);
    }

    // -------------------------------------------------------------------------
    // Data loading & helpers
    // -------------------------------------------------------------------------

    private ExportDataset loadDataset(UUID userId, ExportParameters params) {
        List<TaskDefinitionEntity> definitions = List.of();
        List<TaskOccurrenceEntity> occurrences = List.of();
        List<GoalEntity> goals = List.of();

        if (params.scope().equals("TASKS") || params.scope().equals("FULL")) {
            definitions = taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
            goals = goalRepository.findAllByUserId(userId);
        }
        if (params.scope().equals("HISTORY") || params.scope().equals("FULL")) {
            occurrences = taskOccurrenceRepository
                    .findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                            userId, "canceled", params.fromDate(), params.toDate());
        }
        return new ExportDataset(definitions, occurrences, goals);
    }

    private ExportParameters normalize(UserEntity user, ExportRequest request) {
        String format = request.format() == null ? "EXCEL" : request.format().toUpperCase(Locale.ROOT);
        String scope  = request.scope()  == null ? "FULL"  : request.scope().toUpperCase(Locale.ROOT);
        LocalDate accountCreatedOn = user.getCreatedAt()
                .atZone(ZoneId.of("Europe/Paris")).toLocalDate();
        LocalDate fromDate = request.fromDate() == null || request.fromDate().isBlank()
                ? accountCreatedOn
                : LocalDate.parse(request.fromDate(), DATE_FORMAT);
        LocalDate toDate = request.toDate() == null || request.toDate().isBlank()
                ? LocalDate.now(ZoneId.of("Europe/Paris"))
                : LocalDate.parse(request.toDate(), DATE_FORMAT);

        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("La date de début doit être antérieure ou égale à la date de fin.");
        }
        if (fromDate.isBefore(accountCreatedOn)) {
            fromDate = accountCreatedOn;
        }
        return new ExportParameters(format, scope, fromDate, toDate);
    }

    private String buildFileName(ExportParameters params) {
        String ext = params.format().equals("EXCEL") ? "xlsx" : "pdf";
        return "export-" + params.scope().toLowerCase(Locale.ROOT)
                + "-" + params.fromDate() + "-" + params.toDate() + "." + ext;
    }

    private UserEntity getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
    }

    private ExportAuditItem toItem(ExportAuditEntity audit) {
        return new ExportAuditItem(
                audit.getId().toString(),
                audit.getExportFormat(),
                audit.getExportScope(),
                audit.getPeriodFrom()  == null ? null : audit.getPeriodFrom().toString(),
                audit.getPeriodTo()    == null ? null : audit.getPeriodTo().toString(),
                audit.getStatus(),
                audit.getFileName(),
                audit.getRowCount(),
                audit.getDurationMs(),
                audit.getCreatedAt()   == null ? null : audit.getCreatedAt().toString(),
                audit.getCompletedAt() == null ? null : audit.getCompletedAt().toString()
        );
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "done"      -> "Réalisée";
            case "missed"    -> "Manquée";
            case "suspended" -> "Suspendue";
            case "skipped"   -> "Ignorée";
            case "planned"   -> "Planifiée";
            case "canceled"  -> "Annulée";
            default          -> status;
        };
    }

    private String taskTypeLabel(String type) {
        if (type == null) return "";
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "ONE_TIME"  -> "Ponctuelle";
            case "RECURRING" -> "Récurrente";
            default          -> type;
        };
    }

    private String periodTypeLabel(String period) {
        if (period == null) return "";
        return switch (period.toUpperCase(Locale.ROOT)) {
            case "WEEKLY"  -> "Hebdomadaire";
            case "MONTHLY" -> "Mensuel";
            default        -> period;
        };
    }

    private String goalScopeLabel(String scope) {
        if (scope == null) return "";
        return switch (scope.toUpperCase(Locale.ROOT)) {
            case "GLOBAL" -> "Toutes les tâches";
            case "TASK"   -> "Tâche spécifique";
            default       -> scope;
        };
    }

    private String dayCategoryLabel(String category) {
        if (category == null) return "";
        return switch (category.toUpperCase(Locale.ROOT)) {
            case "WORKDAY"         -> "Semaine";
            case "WEEKEND_HOLIDAY" -> "Week-end / Férié";
            case "VACATION"        -> "Vacances";
            default                -> category;
        };
    }

    private String formatInstant(Instant instant) {
        return instant.atZone(ZoneId.of("Europe/Paris"))
                .toLocalDate().toString();
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    public record ExportPayload(String fileName, String contentType, byte[] content) {}

    private record ExportParameters(String format, String scope, LocalDate fromDate, LocalDate toDate) {}

    private record ExportDataset(
            List<TaskDefinitionEntity> taskDefinitions,
            List<TaskOccurrenceEntity> occurrences,
            List<GoalEntity> goals
    ) {
        int totalRows() {
            return taskDefinitions.size() + occurrences.size() + goals.size();
        }
    }

    /** Cached cell styles for Excel workbook. */
    private static class ExcelStyles {
        final XSSFCellStyle title;
        final XSSFCellStyle header;
        final XSSFCellStyle data;
        final XSSFCellStyle dataAlt;
        final XSSFCellStyle summaryLabel;
        final XSSFCellStyle summaryValue;

        ExcelStyles(XSSFWorkbook wb) {
            title = wb.createCellStyle();
            XSSFFont titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            title.setFont(titleFont);
            title.setFillForegroundColor(COL_TITLE_BG);
            title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            title.setAlignment(HorizontalAlignment.CENTER);
            setBorder(title);

            header = wb.createCellStyle();
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(headerFont);
            header.setFillForegroundColor(COL_HEADER_BG);
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            setBorder(header);

            data = wb.createCellStyle();
            setBorder(data);

            dataAlt = wb.createCellStyle();
            dataAlt.setFillForegroundColor(COL_ALT_ROW_BG);
            dataAlt.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorder(dataAlt);

            summaryLabel = wb.createCellStyle();
            XSSFFont labelFont = wb.createFont();
            labelFont.setBold(true);
            summaryLabel.setFont(labelFont);
            setBorder(summaryLabel);

            summaryValue = wb.createCellStyle();
            setBorder(summaryValue);
        }

        private void setBorder(XSSFCellStyle style) {
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        }
    }
}
