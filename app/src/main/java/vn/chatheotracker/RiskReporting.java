package vn.chatheotracker;
/** Reporting guard: do NOT turn uncalibrated heuristic samples into probabilities. */
final class RiskReporting {
    static String describe(double selected, double minimum, double maximum, int samples) {
        if (samples<100 || !Double.isFinite(selected) || !Double.isFinite(minimum) ||
            !Double.isFinite(maximum) || selected<0 || selected>1)
            return "Chặn: chưa đủ căn cứ";
        // With 192 draws the worst-case sampling error alone is ~7pp; model bias
        // is unknown. Do not use a percentage or a risk label near saturation.
        if (minimum>=0.90 && maximum>=0.90)
            return "Chặn: mô phỏng bão hòa; không phân biệt được nước đánh";
        if(maximum-minimum<0.10)
            return "Chặn: các nước gần tương đương trong mô phỏng";
        return "Chặn: chỉ số so sánh chưa hiệu chuẩn (không phải xác suất)";
    }
    private RiskReporting(){}
}
