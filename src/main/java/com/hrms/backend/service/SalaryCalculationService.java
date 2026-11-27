package com.hrms.backend.service;

import com.hrms.backend.entity.SalaryStructure;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SalaryCalculationService {

    public static final double PF_RATE = 0.12;
    public static final double PF_WAGE_CEILING = 15000.0;
    public static final double ESI_RATE = 0.0175;
    public static final double ESI_GROSS_THRESHOLD = 21000.0;

    // Task N-16: Gross Salary = Basic + HRA + Allowances (safe against negative/zero inputs)
    public double calculateGrossSalary(double basic, double hra, double allowances) {
        double safeBasic = Math.max(0.0, basic);
        double safeHra = Math.max(0.0, hra);
        double safeAllowances = Math.max(0.0, allowances);
        return round2(safeBasic + safeHra + safeAllowances);
    }

    // Tasks N-16 & N-17: Net Salary = Gross - Deductions, clamped to 0.0 (Zero Net Pay guard)
    public double calculateNetSalary(double grossSalary, double totalDeductions) {
        double net = Math.max(0.0, grossSalary) - Math.max(0.0, totalDeductions);
        return round2(Math.max(0.0, net));
    }

    // Task N-17: PF = 12% of Basic (capped at ₹15,000 wage ceiling; returns 0 if Basic <= 0)
    public double calculatePF(double basicSalary) {
        if (basicSalary <= 0.0) {
            return 0.0;
        }
        double pfWage = Math.min(basicSalary, PF_WAGE_CEILING);
        return round2(pfWage * PF_RATE);
    }

    // Task N-17: ESI = 1.75% of Gross if Gross > 0 and Gross < ₹21,000
    public double calculateESI(double grossSalary) {
        if (grossSalary <= 0.0 || grossSalary >= ESI_GROSS_THRESHOLD) {
            return 0.0;
        }
        return round2(grossSalary * ESI_RATE);
    }

    // Task N-17: State-wise monthly Professional Tax (PT) slabs
    public double calculateProfessionalTax(double grossSalary, String state) {
        if (grossSalary <= 0.0) {
            return 0.0;
        }
        String normalizedState = (state == null || state.isBlank()) ? "TELANGANA" : state.trim().toUpperCase();

        return switch (normalizedState) {
            case "TELANGANA", "ANDHRA PRADESH", "AP", "TS" -> {
                if (grossSalary <= 15000.0) yield 0.0;
                else if (grossSalary <= 20000.0) yield 150.0;
                else yield 200.0;
            }
            case "MAHARASHTRA", "MH" -> {
                if (grossSalary <= 7500.0) yield 0.0;
                else if (grossSalary <= 10000.0) yield 175.0;
                else yield 200.0;
            }
            case "KARNATAKA", "KA" -> (grossSalary < 25000.0) ? 0.0 : 200.0;
            case "TAMIL NADU", "TN" -> {
                if (grossSalary <= 21000.0) yield 0.0;
                else if (grossSalary <= 30000.0) yield 135.0;
                else if (grossSalary <= 45000.0) yield 180.0;
                else yield 208.0;
            }
            case "GUJARAT", "GJ" -> (grossSalary < 12000.0) ? 0.0 : 200.0;
            case "WEST BENGAL", "WB" -> {
                if (grossSalary <= 10000.0) yield 0.0;
                else if (grossSalary <= 15000.0) yield 110.0;
                else if (grossSalary <= 25000.0) yield 130.0;
                else if (grossSalary <= 40000.0) yield 150.0;
                else yield 200.0;
            }
            default -> (grossSalary <= 15000.0) ? 0.0 : 200.0;
        };
    }

    // LOP Deduction = (Gross / Total Working Days) * LOP Days
    public double calculateLopDeduction(double grossSalary, int lopDays, int totalWorkingDays) {
        if (grossSalary <= 0.0 || lopDays <= 0 || totalWorkingDays <= 0) {
            return 0.0;
        }
        double effectiveLop = Math.min(lopDays, totalWorkingDays);
        return round2((grossSalary / totalWorkingDays) * effectiveLop);
    }

    // Full breakdown from raw salary amounts (used by N-20, V-23, T-17)
    public Map<String, Object> calculateSalaryBreakdown(
            double basic,
            double hra,
            double allowances,
            double otherDeductions,
            int lopDays,
            int totalWorkingDays,
            String state) {

        double safeBasic = Math.max(0.0, basic);
        double safeHra = Math.max(0.0, hra);
        double safeAllowances = Math.max(0.0, allowances);
        double safeOtherDeductions = Math.max(0.0, otherDeductions);

        double grossSalary = calculateGrossSalary(safeBasic, safeHra, safeAllowances);
        double pf = calculatePF(safeBasic);
        double esi = calculateESI(grossSalary);
        double pt = calculateProfessionalTax(grossSalary, state);
        double lopDeduction = calculateLopDeduction(grossSalary, lopDays, totalWorkingDays);

        double totalDeductions = round2(pf + esi + pt + lopDeduction + safeOtherDeductions);
        double netSalary = calculateNetSalary(grossSalary, totalDeductions);
        boolean zeroNetPayTriggered = (grossSalary - totalDeductions) <= 0.0 && grossSalary > 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("basic", round2(safeBasic));
        result.put("hra", round2(safeHra));
        result.put("allowances", round2(safeAllowances));
        result.put("grossSalary", grossSalary);
        result.put("pf", pf);
        result.put("esi", esi);
        result.put("professionalTax", pt);
        result.put("lopDays", Math.max(0, lopDays));
        result.put("lopDeduction", lopDeduction);
        result.put("otherDeductions", round2(safeOtherDeductions));
        result.put("totalDeductions", totalDeductions);
        result.put("netSalary", netSalary);
        result.put("zeroNetPayTriggered", zeroNetPayTriggered);
        result.put("state", (state == null || state.isBlank()) ? "Telangana" : state);
        return result;
    }

    // Convenience overload reading components directly from a SalaryStructure entity
    public Map<String, Object> calculateFromStructure(
            SalaryStructure structure,
            int lopDays,
            int totalWorkingDays,
            double otherDeductions,
            String state) {

        double basic = 0.0;
        double hra = 0.0;
        double allowances = 0.0;

        if (structure != null && structure.getComponents() != null) {
            for (Map.Entry<String, Double> entry : structure.getComponents().entrySet()) {
                String key = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase();
                double val = entry.getValue() == null ? 0.0 : Math.max(0.0, entry.getValue());

                if (key.equals("basic") || key.equals("basic salary") || key.equals("basic pay")) {
                    basic += val;
                } else if (key.equals("hra") || key.contains("house rent")) {
                    hra += val;
                } else {
                    allowances += val;
                }
            }
        }

        return calculateSalaryBreakdown(basic, hra, allowances, otherDeductions, lopDays, totalWorkingDays, state);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}