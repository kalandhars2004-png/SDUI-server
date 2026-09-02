package com.sdui.server.graphql;

import com.sdui.server.model.Transaction;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class TransactionController {

    @QueryMapping
    public List<Transaction> recentTransactions() {
        return List.of(
            new Transaction("1", "UPI Payment to John", "250.00", "debit", "2024-01-15"),
            new Transaction("2", "Salary Credit", "50000.00", "credit", "2024-01-10"),
            new Transaction("3", "Grocery Shopping", "1200.50", "debit", "2024-01-12"),
            new Transaction("4", "Electricity Bill", "850.00", "debit", "2024-01-14"),
            new Transaction("5", "Freelance Income", "15000.00", "credit", "2024-01-08")
        );
    }
}
