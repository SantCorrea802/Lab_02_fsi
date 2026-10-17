package com.udea.airline.api;

import java.util.List;

public record ApiError(String message, List<String> details) {
}
