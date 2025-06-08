package org.tinkerhub.offgo.service.impl;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tinkerhub.offgo.entity.Attraction;
import org.tinkerhub.offgo.entity.Tag;
import org.tinkerhub.offgo.repository.AttractionRepository;
import org.tinkerhub.offgo.dao.TagDao;
import org.tinkerhub.offgo.service.AttractionService;
import org.tinkerhub.offgo.service.TagService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

// ... rest of the code remains the same ... 