package com.rct.dormfinder.models;

public class FAQItem {
    public static final int TYPE_CATEGORY = 0;
    public static final int TYPE_QUESTION = 1;

    private String question;
    private String answer;
    private int type;
    private boolean isExpanded;

    public FAQItem(String question, String answer, int type) {
        this.question = question;
        this.answer = answer;
        this.type = type;
        this.isExpanded = false;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
}
