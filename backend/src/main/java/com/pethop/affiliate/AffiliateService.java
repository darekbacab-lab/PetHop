package com.pethop.affiliate;

import com.pethop.affiliate.dto.AffiliateRequest;
import com.pethop.affiliate.dto.AffiliateResponse;
import com.pethop.common.exception.ApiException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AffiliateService {
    private final AffiliateRepository affiliates;
    public AffiliateService(AffiliateRepository affiliates){this.affiliates=affiliates;}
    @Transactional(readOnly=true) public List<AffiliateResponse> listPublic(){return affiliates.findAllByActiveTrueOrderByCategoryAscNameAsc().stream().map(AffiliateResponse::from).toList();}
    @Transactional(readOnly=true) public List<AffiliateResponse> listAdmin(){return affiliates.findAllByOrderByCategoryAscNameAsc().stream().map(AffiliateResponse::from).toList();}
    @Transactional public AffiliateResponse create(AffiliateRequest request){return AffiliateResponse.from(save(new Affiliate(),request));}
    @Transactional public AffiliateResponse update(UUID id,AffiliateRequest request){return AffiliateResponse.from(save(affiliates.findById(id).orElseThrow(()->ApiException.notFound("Comercio afiliado no encontrado.")),request));}
    @Transactional public void delete(UUID id){if(!affiliates.existsById(id))throw ApiException.notFound("Comercio afiliado no encontrado.");affiliates.deleteById(id);}
    private Affiliate save(Affiliate value,AffiliateRequest request){
        if(request.websiteUrl()!=null&&!request.websiteUrl().isBlank()){try{URI uri=URI.create(request.websiteUrl().trim());if(!List.of("http","https").contains(uri.getScheme()))throw new IllegalArgumentException();}catch(RuntimeException ex){throw ApiException.badRequest("El sitio web debe ser una URL http o https válida.");}}
        value.setName(request.name().trim());value.setCategory(request.category().trim().toUpperCase());value.setDescription(clean(request.description()));value.setLocation(clean(request.location()));value.setWebsiteUrl(clean(request.websiteUrl()));value.setDiscountCode(clean(request.discountCode()));value.setDiscountText(clean(request.discountText()));if(request.active()!=null)value.setActive(request.active());return affiliates.save(value);
    }
    private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
}
