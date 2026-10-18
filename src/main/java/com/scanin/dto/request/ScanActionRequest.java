package com.scanin.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ScanActionRequest {

    // Səbəb həmişə məcburi deyil (adi girişdə boş gəlir), amma yazılanda 500 simvolu keçməməlidir:
    @Size(max = 500, message = "Yazdığınız səbəb çox uzundur (maksimum 500 simvol ola bilər)!")
    private String reason;

    private boolean forceEarlyCheckout;
}