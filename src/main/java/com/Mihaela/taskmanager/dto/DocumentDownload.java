package com.Mihaela.taskmanager.dto;

import java.io.InputStream;

public record DocumentDownload(InputStream stream, String filename) {

}
