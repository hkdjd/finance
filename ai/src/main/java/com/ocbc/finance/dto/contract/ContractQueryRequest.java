package com.ocbc.finance.dto.contract;

import lombok.Builder;
import lombok.Data;

/**
 * 合同查询请求DTO
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@Builder
public class ContractQueryRequest {

    /**
     * 合同编号（模糊查询）
     */
    private String contractNo;

    /**
     * 合同别名（模糊查询）
     */
    private String contractAlias;

    /**
     * 是否已完成
     */
    private Boolean isFinished;

    /**
     * 页码（从0开始）
     */
    private int page;

    /**
     * 每页大小
     */
    private int size;
}
