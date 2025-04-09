package com.urbanspork.common.protocol.dns;

import java.util.List;

public class DohResponse {
    private int status;
    private String TC;
    private String RD;
    private String RA;
    private String AD;
    private String CD;
    private List<DohRecord> answer;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTC() {
        return TC;
    }

    public void setTC(String TC) {
        this.TC = TC;
    }

    public String getRD() {
        return RD;
    }

    public void setRD(String RD) {
        this.RD = RD;
    }

    public String getRA() {
        return RA;
    }

    public void setRA(String RA) {
        this.RA = RA;
    }

    public String getAD() {
        return AD;
    }

    public void setAD(String AD) {
        this.AD = AD;
    }

    public String getCD() {
        return CD;
    }

    public void setCD(String CD) {
        this.CD = CD;
    }

    public List<DohRecord> getAnswer() {
        return answer;
    }

    public void setAnswer(List<DohRecord> answer) {
        this.answer = answer;
    }
}
