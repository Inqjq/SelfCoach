package com.selfcoach.rag.ingestion;

import java.util.ArrayList;
import java.util.List;

/**
 * 中文文档分块器（RAG 入库管线的第一步）。
 *
 * <p>目标：把一篇长文档切成若干小段（chunk），每段要能独立表达一个语义点，便于后续向量化、
 * 检索时命中「最小但完整的回答单元」。</p>
 *
 * <p>策略（对中文友好，不做分词，因为中文没有空格边界）：</p>
 * <ol>
 *   <li>先按「空行 / 换行」把文档拆成分段；</li>
 *   <li>把相邻的短段落拼到一起，凑到接近 {@code maxChars} 的块（保留段落语义连续）；</li>
 *   <li>若某段本身超过 {@code maxChars}，再按 {@code maxChars} 硬切兜底，保证单块不超长。</li>
 * </ol>
 *
 * <p>为什么：中文不能像英文按空格切，按「段落边界」分块语义天然完整；用 {@code maxChars}
 * 兜底既能避免单块过长（撑爆 embedding 模型的输入上限），又能避免过碎（丢独立语义）。</p>
 */
public class DocumentChunker {

    /** 单块目标最大字符数（贴近 embedding 模型输入上限，中文取 500~800 较合适） */
    private final int maxChars;

    public DocumentChunker(int maxChars) {
        this.maxChars = maxChars;
    }

    /** 入口：整篇文档 → 若干文本块 */
    public List<String> chunk(String document) {
        List<String> chunks = new ArrayList<>();
        StringBuilder buf = new StringBuilder();

        for (String para : splitParagraphs(document)) {
            // 当前已拼的内容 + 下一个段落会超过上限 → 先收尾当前块
            if (buf.length() > 0 && buf.length() + para.length() + 1 > maxChars) {
                chunks.add(buf.toString().trim());
                buf.setLength(0);
            }
            // 段落本身超过上限 → 硬切（可能横跨多块）
            while (para.length() > maxChars) {
                chunks.add(para.substring(0, maxChars));
                para = para.substring(maxChars);
            }
            if (para.length() > 0) {
                buf.append(para).append('\n');
            }
        }
        if (buf.length() > 0) {
            chunks.add(buf.toString().trim());
        }
        return chunks;
    }

    /** 拆段落：以空行为段落分隔，丢弃纯空块、去掉每行首尾空白 */
    private List<String> splitParagraphs(String document) {
        List<String> out = new ArrayList<>();
        if (document == null || document.isBlank()) {
            return out;
        }
        StringBuilder p = new StringBuilder();
        for (String line : document.split("\\r?\\n")) {
            if (line.isBlank()) {
                if (p.length() > 0) {
                    out.add(p.toString());
                    p.setLength(0);
                }
            } else {
                p.append(line.trim()).append(' ');
            }
        }
        if (p.length() > 0) {
            out.add(p.toString());
        }
        return out;
    }
}